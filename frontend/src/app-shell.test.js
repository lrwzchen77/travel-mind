import { describe, expect, it } from 'vitest';
import { navigationItems } from './layout/menu.js';
import { routes } from './router/index.js';
import { createHttpClient } from './api/http.js';

describe('application shell contracts', () => {
  it('exposes the expected Phase 1 navigation entries', () => {
    expect(navigationItems.map((item) => item.path)).toEqual([
      '/',
      '/planning',
      '/profile',
      '/cities',
      '/attractions',
      '/hotels',
      '/restaurants',
      '/trip-history',
      '/favorites',
      '/travel-notes',
      '/ai-lab',
      '/ai-records',
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
    const client = createHttpClient({ baseURL: 'http://localhost:18080/api' });
    expect(client.defaults.baseURL).toBe('http://localhost:18080/api');
    expect(client.defaults.timeout).toBe(10000);
  });
});
