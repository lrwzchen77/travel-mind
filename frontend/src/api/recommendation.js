import { http } from './http.js';

function unwrap(response) {
  return response.data?.data ?? response.data;
}

export function createRecommendationApi(client = http) {
  return {
    list(type = 'city', city, limit = 10) {
      const params = { type, limit };
      if (city) params.city = city;
      return client.get('/user/recommendations', { params }).then(unwrap);
    },
    feedback(id, type, feedback) {
      return client.post(`/user/recommendations/${id}/feedback`, null, {
        params: { type, feedback },
      }).then(unwrap);
    },
  };
}

export const recommendationApi = createRecommendationApi();
