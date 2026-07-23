import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import TripDetailView from './TripDetailView.vue';

const mocks = vi.hoisted(() => ({
  addExpense: vi.fn(), chat: vi.fn(), copy: vi.fn(), detail: vi.fn(), expenses: vi.fn(), remove: vi.fn(), removeExpense: vi.fn(),
  createMemory: vi.fn(), tripComfort: vi.fn(), push: vi.fn(), route: { params: { id: '9001' } },
}));

vi.mock('../api/trip.js', () => ({ tripApi: {
  addExpense: mocks.addExpense, chat: mocks.chat, copy: mocks.copy, detail: mocks.detail, expenses: mocks.expenses,
  remove: mocks.remove, removeExpense: mocks.removeExpense,
} }));
vi.mock('../api/ai.js', () => ({ aiApi: { tripComfort: mocks.tripComfort } }));
vi.mock('../api/memory.js', () => ({ memoryApi: { createFromTrip: mocks.createMemory } }));
vi.mock('../components/map/AsyncTravelMap3D.vue', () => ({ default: {
  name: 'TravelMap3DStub',
  props: ['initialTrackPoints'],
  template: '<div class="map-stub" />',
} }));
vi.mock('vue-router', () => ({
  RouterLink: { props: ['to'], template: '<a :data-to="typeof to === \'string\' ? to : to.path"><slot /></a>' },
  useRoute: () => mocks.route, useRouter: () => ({ push: mocks.push }),
}));

const plan = {
  data: { city: '杭州', start_date: '2026-08-01', end_date: '2026-08-02', budget: { total: 1000 }, days: [{
    date: '2026-08-01', day_index: 0, city: '杭州', description: '西湖慢游', attractions: [{ name: '西湖', address: '西湖区' }],
    meals: [{ name: '楼外楼', address: '孤山路' }], hotel: { name: '湖畔酒店', address: '北山街' },
  }] }, graph_data: { nodes: [] },
};
const expenseSummary = { budget: 1000, actual: 88.5, remaining: 911.5, items: [] };
const storage = new Map();
const localStorageMock = {
  getItem: (key) => storage.get(key) || null,
  setItem: (key, value) => storage.set(key, value),
};

function mountPage() { return mount(TripDetailView); }

beforeEach(() => {
  vi.clearAllMocks();
  storage.clear();
  window.sessionStorage.clear();
  vi.stubGlobal('localStorage', localStorageMock);
  mocks.detail.mockResolvedValue(plan);
  mocks.tripComfort.mockResolvedValue({ result_json: { data: { comfort_score: 88, risk_level: 'low', suggestions: [], daily_risks: [] } } });
  mocks.expenses.mockResolvedValue(expenseSummary);
  mocks.addExpense.mockResolvedValue(expenseSummary);
  mocks.createMemory.mockResolvedValue({ id: 3001 });
});

describe('行程详情新增能力', () => {
  it('从计划快照恢复原始编号路线并交给地图展示', async () => {
    mocks.detail.mockResolvedValueOnce({
      ...plan,
      data: {
        ...plan.data,
        route_intent: { city: '杭州', mode: 'soft_order', nodes: [
          { order: 1, type: 'poi', poi_id: 'west-lake', name: '西湖', kind: 'attraction', longitude: 120.1485, latitude: 30.242 },
          { order: 2, type: 'free_point', name: '自定义节点 2', longitude: 120.1152, latitude: 30.2288 },
        ] },
      },
    });
    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.findAll('.saved-route-strip button')).toHaveLength(2);
    expect(wrapper.get('.saved-route-strip').text()).toContain('01西湖');
    expect(wrapper.findComponent({ name: 'TravelMap3DStub' }).props('initialTrackPoints')).toHaveLength(2);
  });

  it('creates an idempotent memory from this trip and opens it', async () => {
    const wrapper = mountPage();
    await flushPromises();
    await wrapper.get('.trip-hero-actions .btn-coral').trigger('click');
    await flushPromises();

    expect(mocks.createMemory).toHaveBeenCalledWith('9001');
    expect(mocks.push).toHaveBeenCalledWith({ path: '/memories/3001', query: { update: '1' } });
  });

  it('shows a usable departure route and persists its checklist', async () => {
    const wrapper = mountPage();
    await flushPromises();
    await wrapper.get('.trip-hero-actions .btn-ghost').trigger('click');

    const navigation = wrapper.get('.trip-departure-stop');
    expect(navigation.attributes('href')).toContain('uri.amap.com/search?keyword=');
    const check = wrapper.get('.trip-departure-check input');
    await check.setValue(true);
    expect(JSON.parse(storage.get('travel-mind-trip-checks-9001'))).toEqual({ route: true });
  });

  it('records an expense, refreshes the summary, and clears the form', async () => {
    const wrapper = mountPage();
    await flushPromises();
    const form = wrapper.get('.trip-expense-form');
    await form.get('input[placeholder="例如：西湖边午餐"]').setValue('楼外楼午餐');
    await form.get('input[placeholder="金额"]').setValue('88.5');
    await form.trigger('submit');
    await flushPromises();

    expect(mocks.addExpense).toHaveBeenCalledWith('9001', { category: 'food', title: '楼外楼午餐', amount: 88.5, spent_on: '' });
    expect(form.get('input[placeholder="例如：西湖边午餐"]').element.value).toBe('');
    expect(wrapper.text()).toContain('已经花了 ¥88.50');
  });

  it('keeps the trip readable when the optional expense service is unavailable', async () => {
    mocks.expenses.mockRejectedValueOnce(new Error('费用服务不可用'));
    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('杭州');
    expect(wrapper.text()).toContain('实际花费暂时不可用，稍后可以再试。');
  });

  it('ignores a corrupted local checklist instead of blocking the trip', async () => {
    storage.set('travel-mind-trip-checks-9001', '{not-json');
    const wrapper = mountPage();
    await flushPromises();
    expect(wrapper.text()).toContain('杭州');
    await wrapper.get('.trip-hero-actions .btn-ghost').trigger('click');
    expect(wrapper.findAll('.trip-departure-check').length).toBeGreaterThan(0);
  });

  it('deletes an expense only after confirmation and reloads the totals', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    mocks.expenses.mockResolvedValueOnce({ budget: 1000, actual: 88.5, remaining: 911.5, items: [{ id: 7001, category: 'food', title: '午餐', amount: 88.5 }] })
      .mockResolvedValueOnce(expenseSummary);
    mocks.removeExpense.mockResolvedValue({});
    const wrapper = mountPage();
    await flushPromises();
    await wrapper.get('.trip-expense-list button').trigger('click');
    await flushPromises();

    expect(window.confirm).toHaveBeenCalledWith('删掉这笔花费？');
    expect(mocks.removeExpense).toHaveBeenCalledWith('9001', 7001);
    expect(mocks.expenses).toHaveBeenCalledTimes(2);
    expect(wrapper.find('.trip-expense-list').exists()).toBe(false);
  });

  it('marks an over-budget trip and keeps failed expense input for retry', async () => {
    mocks.expenses.mockResolvedValueOnce({ budget: 1000, actual: 1020, remaining: -20, items: [] });
    mocks.addExpense.mockRejectedValueOnce(new Error('网络断开'));
    const wrapper = mountPage();
    await flushPromises();
    expect(wrapper.get('.trip-expense-head > strong').classes()).toContain('is-over');

    const form = wrapper.get('.trip-expense-form');
    const title = form.get('input[placeholder="例如：西湖边午餐"]');
    await title.setValue('晚餐');
    await form.get('input[placeholder="金额"]').setValue('60');
    await form.trigger('submit');
    await flushPromises();
    expect(wrapper.get('.trip-expense .error-line').text()).toBe('网络断开');
    expect(title.element.value).toBe('晚餐');
  });

  it('turns an AI answer into a new planning request without overwriting this trip', async () => {
    mocks.chat.mockResolvedValueOnce({ reply: '第二天删掉一个景点，保留西湖。' });
    const wrapper = mountPage();
    await flushPromises();
    await wrapper.get('.trip-chat-panel textarea').setValue('第二天少走一点');
    await wrapper.get('.trip-chat-panel .actions button').trigger('click');
    await flushPromises();
    await wrapper.get('.chat-bubble:not(.is-user) button').trigger('click');

    expect(mocks.chat).toHaveBeenCalledWith('9001', '第二天少走一点', []);
    expect(mocks.push).toHaveBeenCalledWith({ path: '/map', query: { city: '杭州', assistant: '第二天删掉一个景点，保留西湖。' } });
  });

  it('puts the daily route and map before budget tools', async () => {
    const wrapper = mountPage();
    await flushPromises();
    await wrapper.get('.trip-hero-actions .btn-ghost').trigger('click');
    const html = wrapper.html();

    expect(html.indexOf('trip-route-section')).toBeLessThan(html.indexOf('trip-departure glass-panel'));
    expect(html.indexOf('trip-departure glass-panel')).toBeLessThan(html.indexOf('trip-map-panel'));
    expect(html.indexOf('trip-map-panel')).toBeLessThan(html.indexOf('trip-expense glass-panel'));
  });
});
