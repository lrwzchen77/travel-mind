import { http } from './http.js';
import { uploadApi } from './upload.js';

function unwrap(response) {
  return response.data?.data ?? response.data;
}

export function createJournalApi(client = http, uploader = uploadApi) {
  return {
    list(params = {}) { return client.get('/user/journals', { params }).then(unwrap); },
    create(payload) { return client.post('/user/journals', payload).then(unwrap); },
    detail(journalId) { return client.get(`/user/journals/${journalId}`).then(unwrap); },
    update(journalId, payload) { return client.put(`/user/journals/${journalId}`, payload).then(unwrap); },
    remove(journalId) { return client.delete(`/user/journals/${journalId}`).then(unwrap); },
    publish(journalId) { return client.post(`/user/journals/${journalId}/publish`).then(unwrap); },
    createFromTrip(tripId) { return client.post(`/user/trip/${tripId}/journal`).then(unwrap); },
    addPhoto(journalId, payload) { return client.post(`/user/journals/${journalId}/photos`, payload).then(unwrap); },
    removePhoto(journalId, photoId) { return client.delete(`/user/journals/${journalId}/photos/${photoId}`).then(unwrap); },
    addLocation(journalId, payload) { return client.post(`/user/journals/${journalId}/locations`, payload).then(unwrap); },
    removeLocation(journalId, locationId) { return client.delete(`/user/journals/${journalId}/locations/${locationId}`).then(unwrap); },
    async uploadAndAddPhoto(journalId, file) {
      const upload = await uploader.image(file);
      return this.addPhoto(journalId, { photoUrl: upload.url, caption: '' });
    },
  };
}

export function journalImageUrl(path) {
  if (!path || !String(path).startsWith('/uploads/')) return path || '';
  const api = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
  return `${api.replace(/\/api\/?$/, '')}${path}`;
}

export const journalApi = createJournalApi();
