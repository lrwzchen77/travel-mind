import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createAuthApi } from './auth.js';
import { authSession } from '../auth/session.js';

describe('authentication API', () => {
  beforeEach(() => authSession.clear());

  it('uses separate portal login endpoints and stores the token session', async () => {
    const session = { tokenName: 'Authorization', tokenValue: 'token-1', user: { roles: ['admin'] } };
    const http = { post: vi.fn().mockResolvedValue({ data: { data: session } }) };

    await createAuthApi(http).login('admin', { username: 'admin', password: 'secret' });

    expect(http.post).toHaveBeenCalledWith('/admin/auth/login', { username: 'admin', password: 'secret' });
    expect(authSession.token()).toBe('token-1');
    expect(authSession.hasRole('admin')).toBe(true);
  });

  it('registers a consumer and stores its session', async () => {
    const session = { tokenName: 'Authorization', tokenValue: 'token-2', user: { roles: ['user'] } };
    const http = { post: vi.fn().mockResolvedValue({ data: { data: session } }) };
    const profile = { username: 'traveler', nickname: '旅行者', password: 'secure-password' };

    await createAuthApi(http).register(profile);

    expect(http.post).toHaveBeenCalledWith('/user/auth/register', profile);
    expect(authSession.token()).toBe('token-2');
  });

  it('always discards the local JWT when logout finishes', async () => {
    authSession.save({ tokenValue: 'jwt', user: { roles: ['user'] } });
    const http = { post: vi.fn().mockRejectedValue(new Error('offline')) };

    await expect(createAuthApi(http).logout('user')).rejects.toThrow('offline');

    expect(authSession.isLoggedIn()).toBe(false);
  });
});
