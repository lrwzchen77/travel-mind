import axios from 'axios';
import { authSession } from '../auth/session.js';

const defaultBaseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export function createHttpClient(options = {}) {
  const client = axios.create({
    baseURL: options.baseURL || defaultBaseURL,
    timeout: options.timeout || 10000,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });
  client.interceptors.request.use((config) => {
    if (config.data instanceof FormData) config.headers.setContentType(undefined);
    const token = authSession.token();
    if (token) config.headers.Authorization = token;
    return config;
  });
  client.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error?.response?.status === 401) authSession.clear();
      const message = error?.response?.data?.message;
      if (message) error.message = message;
      return Promise.reject(error);
    },
  );
  return client;
}

export const http = createHttpClient();
