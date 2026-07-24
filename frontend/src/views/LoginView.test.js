import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import LoginView from './LoginView.vue';

const mocks = vi.hoisted(() => ({ login: vi.fn(), replace: vi.fn(), route: { meta: { portal: 'admin' }, query: { redirect: '/' } } }));

vi.mock('../api/auth.js', () => ({ authApi: { login: mocks.login } }));
vi.mock('vue-router', () => ({ RouterLink: { template: '<a><slot /></a>' }, useRoute: () => mocks.route, useRouter: () => ({ replace: mocks.replace }) }));

describe('管理员登录', () => {
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
});
