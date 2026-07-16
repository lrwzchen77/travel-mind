import { http } from './http.js';

function unwrap(response) {
  return response.data?.data ?? response.data;
}

export function createResourceApi(client = http) {
  return {
    discover(resourceKey, params = {}) {
      return client.get(`/public/resources/${resourceKey}`, { params }).then(unwrap);
    },
    list(resourceKey, params = {}) {
      return client.get(`/admin/resources/${resourceKey}`, { params }).then(unwrap);
    },
    detail(resourceKey, id) {
      return client.get(`/admin/resources/${resourceKey}/${id}`).then(unwrap);
    },
    create(resourceKey, payload) {
      return client.post(`/admin/resources/${resourceKey}`, payload).then(unwrap);
    },
    update(resourceKey, id, payload) {
      return client.put(`/admin/resources/${resourceKey}/${id}`, payload).then(unwrap);
    },
    updateStatus(resourceKey, id, status) {
      return client.put(`/admin/resources/${resourceKey}/${id}/status`, null, { params: { status } }).then(unwrap);
    },
    remove(resourceKey, id) {
      return client.delete(`/admin/resources/${resourceKey}/${id}`).then(unwrap);
    },
    userList(resourceKey, params = {}) {
      return client.get(`/user/library/${resourceKey}`, { params }).then(unwrap);
    },
    userCreate(resourceKey, payload) {
      return client.post(`/user/library/${resourceKey}`, payload).then(unwrap);
    },
    userUpdate(resourceKey, id, payload) {
      return client.put(`/user/library/${resourceKey}/${id}`, payload).then(unwrap);
    },
    userRemove(resourceKey, id) {
      return client.delete(`/user/library/${resourceKey}/${id}`).then(unwrap);
    },
    getProfile() {
      return client.get('/user/profile').then(unwrap);
    },
    updateProfile(payload) {
      return client.put('/user/profile', payload).then(unwrap);
    },
  };
}

export const resourceApi = createResourceApi();
