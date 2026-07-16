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
    me(portal) {
      return client.get(`/${portal}/auth/me`).then(unwrap);
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
