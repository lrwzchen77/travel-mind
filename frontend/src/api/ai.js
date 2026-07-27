import { http } from './http.js';

function unwrap(response) {
  return response.data?.data ?? response.data;
}

function params(options = {}) {
  return {
    params: Object.fromEntries(Object.entries(options).filter(([, value]) => value !== undefined && value !== null && value !== '')),
  };
}

export function createAiApi(client = http, portal = 'user') {
  const prefix = `/${portal}/ai`;
  const shared = {
    detectVision(payload) {
      return client.post(`${prefix}/vision/detect`, payload).then(unwrap);
    },
    analyzeContent(payload, options = {}) {
      return client.post(`${prefix}/content/analyze`, payload, params(options)).then(unwrap);
    },
  };
  if (portal === 'admin') return {
    ...shared,
    evaluateTrip(payload, options = {}) {
      return client.post(`${prefix}/trip/evaluate`, payload, params(options)).then(unwrap);
    },
    comfortFeedbackStats() {
      return client.get(`${prefix}/travel-comfort/feedback/stats`).then(unwrap);
    },
  };
  return {
    ...shared,
    tripComfort(id) {
      return client.get(`${prefix}/trip/${id}/comfort`).then(unwrap);
    },
    tripComfortFeedback(id) {
      return client.get(`${prefix}/trip/${id}/comfort/feedback`).then(unwrap);
    },
    saveTripComfortFeedback(id, payload) {
      return client.post(`${prefix}/trip/${id}/comfort/feedback`, payload).then(unwrap);
    },
  };
}

export const aiApi = createAiApi();
export const adminAiApi = createAiApi(http, 'admin');
