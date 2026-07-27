import { http } from './http.js';

const unwrap = (response) => response.data?.data ?? response.data;

export const notificationApi = {
  list: () => http.get('/user/notifications').then(unwrap),
  read: (id) => http.post(`/user/notifications/${id}/read`).then(unwrap),
  readAll: () => http.post('/user/notifications/read-all').then(unwrap),
};
