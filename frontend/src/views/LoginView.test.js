import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LoginView from './LoginView.vue';

const mocks = vi.hoisted(() => ({ login: vi.fn(), register: vi.fn(), replace: vi.fn(), route: { meta: { portal: 'admin' }, query: { redirect: '/' } } }));

vi.mock('../api/auth.js', () => ({ authApi: { login: mocks.login, register: mocks.register } }));
vi.mock('vue-router', () => ({ RouterLink: { template: '<a><slot /></a>' }, useRoute: () => mocks.route, useRouter: () => ({ replace: mocks.replace }) }));

describe('管理员登录', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.route.meta.portal = 'admin';
    mocks.route.query = { redirect: '/' };
  });

  it('ignores a user portal redirect', async () => {
    mocks.login.mockResolvedValue({});
    const wrapper = mount(LoginView);
    await wrapper.get('input[autocomplete="username"]').setValue('admin');
    await wrapper.get('input[type="password"]').setValue('secret');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(mocks.login).toHaveBeenCalledWith('admin', { username: 'admin', password: 'secret' });
    expect(mocks.replace).toHaveBeenCalledWith('/admin');
  });

  it('allows a consumer to register and enters the app', async () => {
    mocks.route.meta.portal = 'user';
    mocks.register.mockResolvedValue({});
    const wrapper = mount(LoginView);
    await wrapper.get('button.btn-ghost').trigger('click');
    await wrapper.get('input[autocomplete="username"]').setValue('traveler');
    await wrapper.get('input[autocomplete="name"]').setValue('旅行者');
    const passwords = wrapper.findAll('input[type="password"]');
    await passwords[0].setValue('secure-password');
    await passwords[1].setValue('secure-password');
    await wrapper.get('form').trigger('submit');
    await flushPromises();

    expect(mocks.register).toHaveBeenCalledWith({ username: 'traveler', nickname: '旅行者', password: 'secure-password' });
    expect(mocks.replace).toHaveBeenCalledWith('/');
  });
});
