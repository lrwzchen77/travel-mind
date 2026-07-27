import { http } from './http.js';
import { authSession } from '../auth/session.js';

function unwrap(response) {
  return response.data?.data ?? response.data;
}

export function createAuthApi(client = http) {
  return {
    async login(portal, credentials) {
      const session = await client.post(`/${portal}/auth/login`, credentials).then(unwrap);
      return authSession.save(session);
    },
    async register(profile) {
      const session = await client.post('/user/auth/register', profile).then(unwrap);
      return authSession.save(session);
    },
    async me(portal) {
      const user = await client.get(`/${portal}/auth/me`).then(unwrap);
      authSession.updateUser(user);
      return user;
    },
    async logout(portal) {
      try {
        await client.post(`/${portal}/auth/logout`);
      } finally {
        authSession.clear();
      }
    },
  };
}

export const authApi = createAuthApi();

export async function refreshAuthSession(api = authApi) {
  if (!authSession.isLoggedIn()) return false;
  const portal = authSession.hasRole('admin') ? 'admin' : 'user';
  try {
    await api.me(portal);
    return true;
  } catch (error) {
    if ([401, 403].includes(error?.response?.status)) authSession.clear();
    return false;
  }
}
