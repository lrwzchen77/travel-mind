import axios from 'axios';

const defaultBaseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:18080/api';

export function createHttpClient(options = {}) {
  return axios.create({
    baseURL: options.baseURL || defaultBaseURL,
    timeout: options.timeout || 10000,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });
}

export const http = createHttpClient();
