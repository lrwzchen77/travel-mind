import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import CityDetailView from './CityDetailView.vue';

const mocks = vi.hoisted(() => ({ discover: vi.fn(), push: vi.fn() }));

vi.mock('../api/resources.js', () => ({ resourceApi: { discover: mocks.discover } }));
vi.mock('../composables/useFavorites.js', () => ({
  useFavorites: () => ({
    busyKey: null,
    isFavorite: () => false,
    loadFavorites: vi.fn(),
    toggleFavorite: vi.fn(),
  }),
}));
vi.mock('vue-router', () => ({
  RouterLink: { props: ['to'], template: '<a :data-to="JSON.stringify(to)"><slot /></a>' },
  useRoute: () => ({ params: { city: '杭州' }, fullPath: '/city/杭州' }),
  useRouter: () => ({ push: mocks.push }),
}));

beforeEach(() => {
  vi.clearAllMocks();
  mocks.discover.mockImplementation((key, params) => {
    if (key === 'cities') return Promise.resolve({ records: [{ id: 11, name: '杭州', province: '浙江', country: '中国' }] });
    if (key === 'attractions') return Promise.resolve({ records: [{ id: 21, name: '西湖', address: '西湖区', opening_hours: '全天', price: 0 }] });
    if (key === 'hotels') return Promise.resolve({ records: [{ id: 22, name: '湖畔酒店', address: '北山街', price_range: '¥500–800' }] });
    if (key === 'restaurants') return Promise.resolve({ records: [{ id: 23, name: '楼外楼', address: '孤山路', average_cost: 160 }] });
    return Promise.resolve({ records: [], params });
  });
});

describe('城市详情信息架构', () => {
  it('按城市过滤并在城内展示玩、吃、住真实信息', async () => {
    const wrapper = mount(CityDetailView);
    await flushPromises();

    for (const key of ['attractions', 'hotels', 'restaurants']) {
      expect(mocks.discover).toHaveBeenCalledWith(key, { cityId: 11, pageSize: 12 });
    }
    expect(wrapper.text()).toContain('西湖区');
    expect(wrapper.text()).toContain('开放时间 全天');
    expect(wrapper.text()).toContain('人均约 ¥160');
    expect(wrapper.text()).not.toMatch(/模型|置信度/);
  });
});
