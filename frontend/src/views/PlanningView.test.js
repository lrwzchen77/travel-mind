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
vi.mock('../components/map/AsyncTravelMap3D.vue', () => ({ default: { template: '<div />' } }));

function dateAfter(days) {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() + days);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

beforeEach(() => {
  vi.clearAllMocks();
  window.sessionStorage.clear();
  mocks.route.query = {};
  mocks.route.fullPath = '/planning';
  mocks.isLoggedIn.mockReturnValue(false);
  mocks.getProfile.mockResolvedValue({});
  mocks.submitPlan.mockResolvedValue({ task_id: 'task-1' });
  mocks.waitForTripTask.mockResolvedValue({
    status: 'completed',
    result: { plan_id: '1', data: { city: '杭州', days: [], budget: {} } },
  });
});

describe('规划页 P0 闭环', () => {
  it('匿名提交时保存草稿并在登录后返回当前规划地址', async () => {
    const wrapper = mount(PlanningView);
    await flushPromises();
    await wrapper.get('#planning-origin').setValue('上海');
    await wrapper.get('#planning-city').setValue('成都');
    await wrapper.get('#planning-days').setValue('3');
    await wrapper.find('textarea').setValue('带父母，少走路');
    await wrapper.find('form').trigger('submit');

    expect(mocks.submitPlan).not.toHaveBeenCalled();
    expect(mocks.push).toHaveBeenCalledWith({ path: '/login', query: { redirect: '/planning' } });
    expect(JSON.parse(window.sessionStorage.getItem('travelmind.planning-draft'))).toMatchObject({
      origin: '上海', city: '成都', travel_days: 3, free_text_input: '带父母，少走路',
    });
  });

  it('恢复草稿和偏好后仍以显式 query 为准，并避免重复追加线索', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    mocks.route.query = { city: '苏州', note: '必须去拙政园', travel_days: '4', preferences: '夜景,拍照', vision: '夜景画面', model: 'local' };
    window.sessionStorage.setItem('travelmind.planning-draft', JSON.stringify({
      city: '厦门', travel_days: 3, free_text_input: '必须去拙政园', preferences: ['轻松'],
    }));
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
    expect(wrapper.get('#planning-city').element.value).toBe('苏州');
    expect(wrapper.get('#planning-days').element.value).toBe('4');
    expect(wrapper.find('textarea').element.value.match(/必须去拙政园/g)).toHaveLength(1);
    expect(wrapper.find('textarea').element.value).toContain('照片场景偏好：喜欢夜景画面');
    expect(wrapper.find('textarea').element.value).not.toContain('模型');
    expect(wrapper.findAll('.chip-choice.is-on').map((chip) => chip.text())).toEqual(['夜景', '拍照']);
    expect(window.sessionStorage.getItem('travelmind.planning-draft')).toBeNull();
  });

  it('只提交出发日期和天数推导出的返程日期', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    mocks.route.query = { origin: '上海', start_date: dateAfter(10), travel_days: '3' };
    const wrapper = mount(PlanningView);
    await flushPromises();
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    expect(wrapper.find('input[readonly]').element.value).toBe(dateAfter(12));
    expect(mocks.submitPlan).toHaveBeenCalledWith(expect.objectContaining({
      start_date: dateAfter(10), end_date: dateAfter(12), travel_days: 3,
    }));
  });

  it('前端拒绝过去日期', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    mocks.route.query = { origin: '上海' };
    const wrapper = mount(PlanningView);
    await flushPromises();
    await wrapper.get('#planning-start-date').setValue(dateAfter(-1));
    await wrapper.find('form').trigger('submit');

    expect(wrapper.text()).toContain('出发日期不能早于今天');
    expect(mocks.submitPlan).not.toHaveBeenCalled();
  });

  it('把同行人快捷选择写入现有自由文本', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    mocks.route.query = { origin: '上海' };
    const wrapper = mount(PlanningView);
    await flushPromises();
    const companion = wrapper.findAll('.chip-choice').find((button) => button.text() === '带老人');
    await companion.trigger('click');
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    expect(companion.attributes('aria-pressed')).toBe('true');
    expect(mocks.submitPlan).toHaveBeenCalledWith(expect.objectContaining({
      free_text_input: expect.stringContaining('同行人：带老人'),
    }));
  });

  it('快捷同行人和人数保持一致', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    mocks.route.query = { origin: '上海' };
    const wrapper = mount(PlanningView);
    await flushPromises();

    await wrapper.findAll('.chip-choice').find((button) => button.text() === '独自').trigger('click');
    expect(wrapper.get('#planning-adults').element.value).toBe('1');
    expect(wrapper.get('#planning-children').element.value).toBe('0');

    await wrapper.findAll('.chip-choice').find((button) => button.text() === '带孩子').trigger('click');
    expect(wrapper.get('#planning-children').element.value).toBe('1');
  });

  it('把出发地、人数和预算口径合并进后端已支持的自由文本', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    mocks.route.query = { origin: '南京', adults: '2', children: '1', budget_scope: '人均预算', include_transport: 'false' };
    const wrapper = mount(PlanningView);
    await flushPromises();
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    const payload = mocks.submitPlan.mock.calls[0][0];
    expect(payload.free_text_input).toContain('出发地：南京');
    expect(payload.free_text_input).toContain('同行人数：2 位成人、1 位儿童');
    expect(payload.free_text_input).toContain('预算口径：人均预算，不包含往返大交通');
    expect(payload).not.toHaveProperty('origin');
    expect(payload).not.toHaveProperty('adults');
    expect(payload).not.toHaveProperty('budget_scope');
  });

  it('恢复草稿时拒绝未知预算口径', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    window.sessionStorage.setItem('travelmind.planning-draft', JSON.stringify({
      origin: '上海', city: '杭州', budget_scope: '每晚预算',
    }));
    const wrapper = mount(PlanningView);
    await flushPromises();
    await wrapper.find('form').trigger('submit');

    expect(wrapper.text()).toContain('请选择预算口径');
    expect(mocks.submitPlan).not.toHaveBeenCalled();
  });

  it('真实吃住玩资源未补齐时不用占位商户生成', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    mocks.route.query = { origin: '上海', city: '厦门' };
    const wrapper = mount(PlanningView);
    await flushPromises();
    await wrapper.find('form').trigger('submit');

    expect(wrapper.text()).toContain('不会用占位商户生成行程');
    expect(mocks.submitPlan).not.toHaveBeenCalled();
  });

  it('不向消费者展示内部规划器名称', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    mocks.route.query = { origin: '上海' };
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
