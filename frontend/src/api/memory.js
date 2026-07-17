import { http } from './http.js';
import { uploadApi } from './upload.js';

function unwrap(response) {
  return response.data?.data ?? response.data;
}

export function createMemoryApi(client = http, uploader = uploadApi) {
  return {
    createFromTrip(tripId) { return client.post(`/user/trips/${tripId}/memory`).then(unwrap); },
    list(params = {}) { return client.get('/user/memories', { params }).then(unwrap); },
    detail(memoryId) { return client.get(`/user/memories/${memoryId}`).then(unwrap); },
    async addPhotos(memoryId, files) {
      const added = [];
      for (const file of files) {
        const upload = await uploader.image(file);
        added.push(await client.post(`/user/memories/${memoryId}/items/photos`, { url: upload.url }).then(unwrap));
      }
      return added;
    },
    analyze(memoryId) { return client.post(`/user/memories/${memoryId}/analyze`).then(unwrap); },
    index(memoryId) { return client.post(`/user/memories/${memoryId}/index`).then(unwrap); },
    ask(memoryId, question) { return client.post(`/user/memories/${memoryId}/ask`, { question }).then(unwrap); },
    removeItem(memoryId, itemId) { return client.delete(`/user/memories/${memoryId}/items/${itemId}`).then(unwrap); },
    remove(memoryId) { return client.delete(`/user/memories/${memoryId}`).then(unwrap); },
    publish(memoryId, payload) { return client.post(`/user/memories/${memoryId}/publish`, payload).then(unwrap); },
  };
}

export function memoryImageUrl(path) {
  if (!path || !String(path).startsWith('/uploads/')) return path || '';
  const api = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
  return `${api.replace(/\/api\/?$/, '')}${path}`;
}

export const memoryApi = createMemoryApi();
