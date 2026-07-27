import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import DiscoveryListView from './DiscoveryListView.vue';

const mocks = vi.hoisted(() => ({
  discover: vi.fn(),
  route: { meta: { resourceKey: 'attractions', title: '去哪玩' }, query: {}, fullPath: '/attractions' },
}));

vi.mock('../api/resources.js', () => ({ resourceApi: { discover: mocks.discover } }));
vi.mock('../auth/session.js', () => ({ authSession: { isLoggedIn: () => false } }));
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
  useRoute: () => mocks.route,
  useRouter: () => ({ push: vi.fn() }),
}));
vi.mock('../components/VisionInspirationPanel.vue', () => ({ default: { template: '<div />' } }));

beforeEach(() => {
  vi.clearAllMocks();
  mocks.discover.mockImplementation((resourceKey) => Promise.resolve(resourceKey === 'cities'
    ? { records: [{ id: 2, name: '北京' }] }
    : { records: [{ id: 7, city_id: 2, name: '故宫' }], total: 1 }));
});

describe('发现页带入规划', () => {
  it('用已有城市接口把资源名称、类型和城市一起带进规划', async () => {
    const wrapper = mount(DiscoveryListView);
    await flushPromises();

    const to = JSON.parse(wrapper.find('.text-action--primary').attributes('data-to'));
    expect(to).toMatchObject({
      path: '/map',
      query: { city: '北京', cityId: 2, resourceType: 'attractions', resourceName: '故宫' },
    });
    expect(to.query.note).toContain('故宫（北京）');
    expect(JSON.parse(wrapper.get('.discovery-cover').attributes('data-to'))).toBe('/discover/attractions/7');
  });

  it('uses public tags, filters, and backend pagination', async () => {
    mocks.discover.mockImplementation((resourceKey, params = {}) => {
      if (resourceKey === 'travel-tags') return Promise.resolve({ records: [{ id: 9, name: '古建' }] });
      if (resourceKey === 'cities') return Promise.resolve({ records: [{ id: 2, name: '北京' }] });
      return Promise.resolve({ records: [{ id: params.pageNum === 2 ? 8 : 7, city_id: 2, name: params.pageNum === 2 ? '天坛' : '故宫' }], total: 2 });
    });
    const wrapper = mount(DiscoveryListView);
    await flushPromises();
    await wrapper.get('.discovery-filter-row input').setValue('历史文化');
    await wrapper.findAll('.discovery-filter-row select')[0].setValue('古建');
    await wrapper.findAll('.discovery-filter-row select')[1].setValue('4.5');
    await wrapper.get('.discovery-search').trigger('submit');
    await flushPromises();
    expect(mocks.discover).toHaveBeenCalledWith('attractions', expect.objectContaining({ category: '历史文化', tag: '古建', ratingMin: '4.5', pageNum: 1 }));
    await wrapper.get('.load-more button').trigger('click');
    await flushPromises();
    expect(wrapper.text()).toContain('天坛');
  });
});
