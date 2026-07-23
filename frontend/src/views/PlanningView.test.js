import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import PlanningView from './PlanningView.vue';

const mocks = vi.hoisted(() => ({
  getProfile: vi.fn(),
  isLoggedIn: vi.fn(),
  push: vi.fn(),
  route: { query: {}, fullPath: '/planning' },
  submitPlan: vi.fn(),
  waitForTripTask: vi.fn(),
}));

vi.mock('../api/resources.js', () => ({ resourceApi: { getProfile: mocks.getProfile } }));
vi.mock('../api/trip.js', () => ({ tripApi: { submitPlan: mocks.submitPlan, status: vi.fn() } }));
vi.mock('../api/http.js', () => ({ http: { defaults: { baseURL: '/api' } } }));
vi.mock('../auth/session.js', () => ({
  authSession: { isLoggedIn: mocks.isLoggedIn, token: () => 'token' },
}));
vi.mock('../api/tripTask.js', () => ({
  normalizeTripTaskStatus: (status) => status || 'ready',
  TripTaskTimeoutError: class extends Error {},
  waitForTripTask: mocks.waitForTripTask,
}));
vi.mock('vue-router', () => ({
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
  useRoute: () => mocks.route,
  useRouter: () => ({ push: mocks.push }),
}));

function dateAfter(days) {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() + days);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function routeIntent(city = '杭州') {
  return {
    city,
    mode: 'soft_order',
    nodes: [
      {
        order: 1,
        type: 'poi',
        poi_id: 'west-lake',
        name: '西湖',
        kind: 'attraction',
        longitude: 120.1485,
        latitude: 30.242,
        note: '傍晚看日落',
        preferences: ['必去', '拍照'],
      },
      {
        order: 2,
        type: 'free_point',
        name: '龙井路慢游段',
        longitude: 120.1152,
        latitude: 30.2288,
        note: '少走一点',
        preferences: ['慢游'],
      },
    ],
  };
}

function setRoute(query = {}, intent = routeIntent(query.city || '杭州')) {
  mocks.route.query = { city: intent.city, route: '1', ...query };
  mocks.route.fullPath = `/planning?city=${encodeURIComponent(intent.city)}&route=1`;
  window.sessionStorage.setItem('travelmind.route-intent', JSON.stringify(intent));
}

beforeEach(() => {
  vi.clearAllMocks();
  window.sessionStorage.clear();
  mocks.isLoggedIn.mockReturnValue(false);
  mocks.getProfile.mockResolvedValue({});
  mocks.submitPlan.mockResolvedValue({ task_id: 'task-1' });
  mocks.waitForTripTask.mockResolvedValue({
    status: 'completed',
    result: { plan_id: '1', data: { city: '杭州', days: [], budget: {} } },
  });
  setRoute();
});

describe('路线确认页 P0 闭环', () => {
  it('没有地图路线时只引导回地图，不展示确认表单', async () => {
    mocks.route.query = {};
    mocks.route.fullPath = '/planning';
    window.sessionStorage.clear();

    const wrapper = mount(PlanningView);
    await flushPromises();

    expect(wrapper.text()).toContain('先在地图上选好至少两个节点');
    expect(wrapper.find('form').exists()).toBe(false);
    expect(wrapper.text()).toContain('去地图画路线');
  });

  it('匿名提交时保存补充信息并在登录后返回当前确认地址', async () => {
    const wrapper = mount(PlanningView);
    await flushPromises();
    await wrapper.get('#planning-days').setValue('3');
    await wrapper.find('textarea').setValue('带父母，少走路');
    await wrapper.find('form').trigger('submit');

    expect(mocks.submitPlan).not.toHaveBeenCalled();
    expect(mocks.push).toHaveBeenCalledWith({ path: '/login', query: { redirect: '/planning?city=%E6%9D%AD%E5%B7%9E&route=1' } });
    expect(JSON.parse(window.sessionStorage.getItem('travelmind.planning-draft'))).toMatchObject({
      city: '杭州', travel_days: 3, free_text_input: '带父母，少走路',
    });
  });

  it('恢复草稿和偏好后仍以显式 query 为准，并避免重复追加线索', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    window.sessionStorage.setItem('travelmind.planning-draft', JSON.stringify({
      city: '厦门', travel_days: 3, free_text_input: '必须去拙政园', preferences: ['轻松'],
    }));
    setRoute({ city: '苏州', note: '必须去拙政园', travel_days: '4', preferences: '夜景,拍照', vision: '夜景画面', model: 'local' }, routeIntent('苏州'));
    mocks.getProfile.mockResolvedValue({ preference: {
      preferred_city: '成都、杭州',
      preferred_tags: '美食、古镇',
      transportation: '自驾',
      hotel_level: '精品民宿',
      budget_level: 'premium',
      diet_preference: '清淡',
    } });

    const wrapper = mount(PlanningView);
    await flushPromises();
    expect(wrapper.get('.route-locked-summary').text()).toContain('苏州 · 2 个节点');
    expect(wrapper.get('#planning-days').element.value).toBe('4');
    expect(wrapper.find('textarea').element.value.match(/必须去拙政园/g)).toHaveLength(1);
    expect(wrapper.find('textarea').element.value).toContain('照片场景偏好：喜欢夜景画面');
    expect(wrapper.find('textarea').element.value).not.toContain('模型');
    expect(wrapper.findAll('.chip-choice.is-on').map((chip) => chip.text())).toEqual(['夜景', '拍照']);
    expect(window.sessionStorage.getItem('travelmind.planning-draft')).toBeNull();
  });

  it('只提交出发日期和天数推导出的返程日期', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    setRoute({ origin: '上海', start_date: dateAfter(10), travel_days: '3' });
    const wrapper = mount(PlanningView);
    await flushPromises();
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    expect(wrapper.get('.route-locked-summary').text()).toContain(`预计 ${dateAfter(12)} 结束`);
    expect(mocks.submitPlan).toHaveBeenCalledWith(expect.objectContaining({
      start_date: dateAfter(10), end_date: dateAfter(12), travel_days: 3,
    }));
  });

  it('展示节点顺序、备注和偏好，并作为结构化约束提交', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    setRoute({ origin: '上海' });
    const wrapper = mount(PlanningView);
    await flushPromises();

    expect(wrapper.get('.route-review').text()).toContain('01西湖');
    expect(wrapper.get('.route-review').text()).toContain('傍晚看日落');
    expect(wrapper.get('.route-review').text()).toContain('必去拍照');
    expect(wrapper.get('.route-review').text()).toContain('02龙井路慢游段');
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    expect(mocks.submitPlan).toHaveBeenCalledWith(expect.objectContaining({
      route_intent: expect.objectContaining({
        city: '杭州',
        mode: 'soft_order',
        nodes: expect.arrayContaining([expect.objectContaining({ note: '傍晚看日落', preferences: ['必去', '拍照'] })]),
      }),
    }));
    expect(window.sessionStorage.getItem('travelmind.route-intent')).toBeNull();
  });

  it('前端拒绝过去日期', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    setRoute({ origin: '上海' });
    const wrapper = mount(PlanningView);
    await flushPromises();
    await wrapper.get('#planning-start-date').setValue(dateAfter(-1));
    await wrapper.find('form').trigger('submit');

    expect(wrapper.text()).toContain('出发日期不能早于今天');
    expect(mocks.submitPlan).not.toHaveBeenCalled();
  });

  it('把人数和市内预算范围合并进后端已支持的自由文本', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    setRoute({ adults: '2', children: '1', budget: '4200' });
    const wrapper = mount(PlanningView);
    await flushPromises();
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    const payload = mocks.submitPlan.mock.calls[0][0];
    expect(payload.free_text_input).toContain('同行人数：2 位成人、1 位儿童');
    expect(payload.free_text_input).toContain('预算范围：仅计算目的地内的吃住行游，不包含往返目的地的大交通');
    expect(payload.budget).toBe('4200');
    expect(payload).not.toHaveProperty('origin');
    expect(payload).not.toHaveProperty('adults');
    expect(payload).not.toHaveProperty('budget_scope');
  });

  it('不再显示路线已经能推导或业务不再使用的字段', async () => {
    const wrapper = mount(PlanningView);
    await flushPromises();

    expect(wrapper.find('#planning-origin').exists()).toBe(false);
    expect(wrapper.find('#planning-end-date').exists()).toBe(false);
    expect(wrapper.find('#planning-budget-scope').exists()).toBe(false);
    expect(wrapper.find('.planner-check').exists()).toBe(false);
    expect(wrapper.text()).not.toContain('和谁一起');
  });

  it('真实吃住玩资源未补齐时不用占位商户生成', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    setRoute({ origin: '上海', city: '厦门' }, routeIntent('厦门'));
    const wrapper = mount(PlanningView);
    await flushPromises();
    await wrapper.find('form').trigger('submit');

    expect(wrapper.text()).toContain('不会用占位商户生成行程');
    expect(mocks.submitPlan).not.toHaveBeenCalled();
  });

  it('不向消费者展示内部规划器名称', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    setRoute({ origin: '上海' });
    mocks.waitForTripTask.mockResolvedValueOnce({
      status: 'completed',
      result: { plan_id: '1', data: { city: '杭州', days: [], budget: {}, overall_suggestions: 'Demo Planner 基于本地资源库生成。' } },
    });
    const wrapper = mount(PlanningView);
    await flushPromises();
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    expect(wrapper.text()).not.toContain('Demo Planner');
    expect(wrapper.text()).toContain('预算未接入实时交通、价格和库存');
  });
});
