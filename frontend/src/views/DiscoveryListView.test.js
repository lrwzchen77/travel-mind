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
      path: '/planning',
      query: { city: '北京', cityId: 2, resourceType: 'attractions', resourceName: '故宫' },
    });
    expect(to.query.note).toContain('故宫（北京）');
  });
});
