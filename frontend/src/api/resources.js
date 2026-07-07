import { http } from './http.js';

function unwrap(response) {
  return response.data?.data ?? response.data;
}

export function createResourceApi(client = http) {
  return {
    list(resourceKey, params = {}) {
      return client.get(`/${resourceKey}`, { params }).then(unwrap);
    },
    detail(resourceKey, id) {
      return client.get(`/${resourceKey}/${id}`).then(unwrap);
    },
    create(resourceKey, payload) {
      return client.post(`/${resourceKey}`, payload).then(unwrap);
    },
    update(resourceKey, id, payload) {
      return client.put(`/${resourceKey}/${id}`, payload).then(unwrap);
    },
    updateStatus(resourceKey, id, status) {
      return client.put(`/${resourceKey}/${id}/status`, null, { params: { status } }).then(unwrap);
    },
    remove(resourceKey, id) {
      return client.delete(`/${resourceKey}/${id}`).then(unwrap);
    },
    getProfile(userId = 1001) {
      return client.get('/users/profile', { params: { userId } }).then(unwrap);
    },
    updateProfile(userId = 1001, payload) {
      return client.put('/users/profile', payload, { params: { userId } }).then(unwrap);
    },
    tripHistory(limit = 8) {
      return client.get('/trip/history', { params: { limit } }).then(unwrap);
    },
  };
}

export const resourceApi = createResourceApi();
