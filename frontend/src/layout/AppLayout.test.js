import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AppLayout from './AppLayout.vue';

const mocks = vi.hoisted(() => ({
  route: { path: '/', fullPath: '/' },
  replace: vi.fn(),
}));

vi.mock('vue-router', () => ({
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
  useRoute: () => mocks.route,
  useRouter: () => ({ replace: mocks.replace }),
}));
vi.mock('../components/PageTransition.vue', () => ({ default: { template: '<div data-testid="page" />' } }));
vi.mock('../components/InspirationBagFloat.vue', () => ({ default: { template: '<div data-testid="inspiration-bag-float" />' } }));
vi.mock('../api/auth.js', () => ({ authApi: { logout: vi.fn() } }));
vi.mock('../auth/session.js', () => ({
  authSession: { user: () => ({ name: '旅行者' }), hasRole: () => false, isLoggedIn: () => true, clear: vi.fn() },
}));

beforeEach(() => {
  mocks.route.path = '/';
  mocks.route.fullPath = '/';
  vi.clearAllMocks();
});

describe('用户端布局', () => {
  it('hides the unrelated inspiration bag on memory pages only', async () => {
    for (const path of ['/memories', '/memories/996889308694955191']) {
      mocks.route.path = path;
      mocks.route.fullPath = path;
      const wrapper = mount(AppLayout);
      await flushPromises();
      expect(wrapper.find('[data-testid="inspiration-bag-float"]').exists()).toBe(false);
      wrapper.unmount();
    }

    mocks.route.path = '/trip-history';
    mocks.route.fullPath = '/trip-history';
    const wrapper = mount(AppLayout);
    await flushPromises();
    expect(wrapper.find('[data-testid="inspiration-bag-float"]').exists()).toBe(true);
    wrapper.unmount();
  });

  it('在城市和行程详情中保持对应主导航激活', () => {
    mocks.route.path = '/city/杭州';
    let wrapper = mount(AppLayout);
    expect(wrapper.findAll('.top-link').find((link) => link.text() === '目的地').classes()).toContain('is-active');
    wrapper.unmount();

    mocks.route.path = '/trip/123';
    wrapper = mount(AppLayout);
    expect(wrapper.findAll('.top-link').find((link) => link.text() === '我的行程').classes()).toContain('is-active');
  });
});
