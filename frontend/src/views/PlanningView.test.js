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

function inputs(wrapper) {
  return wrapper.find('form').findAll('input');
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
    const fields = inputs(wrapper);
    await fields[0].setValue('厦门');
    await fields[1].setValue('3');
    await wrapper.find('textarea').setValue('带父母，少走路');
    await wrapper.find('form').trigger('submit');

    expect(mocks.submitPlan).not.toHaveBeenCalled();
    expect(mocks.push).toHaveBeenCalledWith({ path: '/login', query: { redirect: '/planning' } });
    expect(JSON.parse(window.sessionStorage.getItem('travelmind.planning-draft'))).toMatchObject({
      city: '厦门', travel_days: 3, free_text_input: '带父母，少走路',
    });
  });

  it('恢复草稿和偏好后仍以显式 query 为准，并避免重复追加线索', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    mocks.route.query = { city: '苏州', note: '必须去拙政园', travel_days: '4', preferences: '夜景,拍照' };
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
    const fields = inputs(wrapper);
    expect(fields[0].element.value).toBe('苏州');
    expect(fields[1].element.value).toBe('4');
    expect(wrapper.find('textarea').element.value.match(/必须去拙政园/g)).toHaveLength(1);
    expect(wrapper.findAll('.chip-choice.is-on').map((chip) => chip.text())).toEqual(['夜景', '拍照']);
    expect(window.sessionStorage.getItem('travelmind.planning-draft')).toBeNull();
  });

  it('只提交出发日期和天数推导出的返程日期', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    mocks.route.query = { start_date: dateAfter(10), travel_days: '3' };
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
    const wrapper = mount(PlanningView);
    await flushPromises();
    await inputs(wrapper)[2].setValue(dateAfter(-1));
    await wrapper.find('form').trigger('submit');

    expect(wrapper.text()).toContain('出发日期不能早于今天');
    expect(mocks.submitPlan).not.toHaveBeenCalled();
  });
});
