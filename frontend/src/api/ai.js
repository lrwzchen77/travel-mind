import { http } from './http.js';

function unwrap(response) {
  return response.data?.data ?? response.data;
}

function params(options = {}) {
  return {
    params: Object.fromEntries(Object.entries(options).filter(([, value]) => value !== undefined && value !== null && value !== '')),
  };
}

export function createAiApi(client = http) {
  return {
    detectVision(payload) {
      return client.post('/ai/vision/detect', payload).then(unwrap);
    },
    evaluateTrip(payload, options = {}) {
      return client.post('/ai/trip/evaluate', payload, params(options)).then(unwrap);
    },
    analyzeContent(payload, options = {}) {
      return client.post('/ai/content/analyze', payload, params(options)).then(unwrap);
    },
    tripComfort(id) {
      return client.get(`/ai/trip/${id}/comfort`).then(unwrap);
    },
  };
}

export const aiApi = createAiApi();
