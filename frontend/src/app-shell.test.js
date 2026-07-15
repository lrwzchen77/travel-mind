import { describe, expect, it } from 'vitest';
import { navigationItems } from './layout/menu.js';
import { routes } from './router/index.js';
import { createHttpClient } from './api/http.js';

describe('application shell contracts', () => {
  it('exposes the expected navigation entries in consumer order', () => {
    expect(navigationItems.map((item) => item.path)).toEqual([
      '/',
      '/planning',
      '/map',
      '/trip-history',
      '/cities',
      '/ai-lab',
      '/attractions',
      '/hotels',
      '/restaurants',
      '/favorites',
      '/travel-notes',
      '/ai-records',
      '/profile',
    ]);
  });

  it('registers router paths for all navigation entries', () => {
    const routePaths = routes.map((route) => route.path);
    for (const item of navigationItems) {
      expect(routePaths).toContain(item.path);
    }
    expect(routePaths).toContain('/trip/:id');
  });

  it('creates an API client with the configured base URL', () => {
    const client = createHttpClient({ baseURL: 'http://localhost:8080/api' });
    expect(client.defaults.baseURL).toBe('http://localhost:8080/api');
    expect(client.defaults.timeout).toBe(10000);
  });
});
