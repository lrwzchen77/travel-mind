import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import AdminLayout from './AdminLayout.vue';

vi.mock('vue-router', () => ({
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
  useRoute: () => ({ path: '/admin', fullPath: '/admin', meta: {} }),
  useRouter: () => ({ replace: vi.fn() }),
}));
vi.mock('../components/PageTransition.vue', () => ({ default: { template: '<div />' } }));
vi.mock('../api/auth.js', () => ({ authApi: { logout: vi.fn() } }));
vi.mock('../auth/session.js', () => ({ authSession: { user: () => ({ name: '管理员' }) } }));

describe('管理端布局', () => {
  it('opens and closes the mobile navigation', async () => {
    const wrapper = mount(AdminLayout);

    expect(wrapper.text()).toContain('用户偏好');

    await wrapper.get('.admin-mobile-toggle').trigger('click');
    expect(wrapper.get('.admin-sidebar').classes()).toContain('is-open');
    expect(wrapper.find('.admin-sidebar-scrim').exists()).toBe(true);

    await wrapper.get('.admin-sidebar-close').trigger('click');
    expect(wrapper.get('.admin-sidebar').classes()).not.toContain('is-open');
  });
});
