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
});
