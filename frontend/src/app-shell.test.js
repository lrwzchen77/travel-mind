import { describe, expect, it } from 'vitest';
import {
  accountNav,
  auxiliaryNav,
  navigationItems,
  primaryNav,
} from './layout/menu.js';
import { routes } from './router/index.js';
import { createHttpClient } from './api/http.js';

describe('application shell contracts', () => {
  it('exposes the expected navigation entries in consumer order', () => {
    expect(navigationItems.map((item) => item.path)).toEqual([
      '/',
      '/cities',
      '/map',
      '/trip-history',
      '/planning',
      '/ai-lab',
      '/ai-records',
      '/favorites',
      '/travel-notes',
      '/profile',
    ]);
    expect(primaryNav.map((item) => item.label)).toEqual([
      '首页', '发现目的地', '立体地图', '我的行程',
    ]);
    expect(auxiliaryNav.map((item) => item.label)).toEqual(['智能规划', 'AI 灵感', '最近灵感']);
    expect(accountNav.map((item) => item.label)).toEqual(['我的收藏', '我的笔记', '旅行偏好']);
  });

  it('registers router paths for all navigation entries', () => {
    const routePaths = routes.flatMap((route) => {
      if (!route.children) return [route.path];
      return route.children.map((child) => {
        if (child.path === '') return route.path;
        return `${route.path === '/' ? '' : route.path}/${child.path}`;
      });
    });
    for (const item of navigationItems) {
      expect(routePaths).toContain(item.path);
    }
    expect(routePaths).toContain('/trip/:id');
    expect(routePaths).toContain('/city/:city');
    expect(routePaths).toContain('/attractions');
    expect(routePaths).toContain('/hotels');
    expect(routePaths).toContain('/restaurants');
    expect(routePaths).toContain('/admin/resources/users');
    expect(routePaths).toContain('/admin/settings');
  });

  it('creates an API client with the configured base URL', () => {
    const client = createHttpClient({ baseURL: 'http://localhost:8080/api' });
    expect(client.defaults.baseURL).toBe('http://localhost:8080/api');
    expect(client.defaults.timeout).toBe(10000);
  });
});
