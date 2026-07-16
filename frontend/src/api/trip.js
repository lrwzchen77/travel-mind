import { http } from './http.js';

function unwrap(response) {
  const body = response.data;
  return body && Object.prototype.hasOwnProperty.call(body, 'code') ? body.data : body;
}

export function createTripApi(client = http) {
  return {
    submitPlan(payload) {
      return client.post('/user/trip/plan', payload).then(unwrap);
    },
    status(taskId) {
      return client.get(`/user/trip/status/${taskId}`).then(unwrap);
    },
    history(limit = 20) {
      return client.get('/user/trip/history', { params: { limit } }).then(unwrap);
    },
    detail(id) {
      return client.get(`/user/trip/${id}`).then(unwrap);
    },
    copy(id) {
      return client.post(`/user/trip/${id}/copy`).then(unwrap);
    },
    remove(id) {
      return client.delete(`/user/trip/${id}`).then(unwrap);
    },
    chat(id, message, history = []) {
      return client.post(`/user/trip/${id}/chat`, { message, history }).then(unwrap);
    },
  };
}

export const tripApi = createTripApi();
