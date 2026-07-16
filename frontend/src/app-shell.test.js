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
      '/inspirations',
      '/assistant',
      '/cities',
      '/trip-history',
      '/planning',
      '/map',
      '/ai-lab',
      '/inspiration-bag',
      '/my-posts',
      '/favorites',
      '/travel-notes',
      '/profile',
    ]);
    expect(primaryNav.map((item) => item.label)).toEqual([
      '首页', '旅行灵感', '先问 AI', '目的地', '我的行程',
    ]);
    expect(auxiliaryNav.map((item) => item.label)).toEqual(['生成行程', '立体地图', 'AI 内容解读']);
    expect(accountNav.map((item) => item.label)).toEqual(['我的灵感包', '我的分享', '我的收藏', '我的笔记', '旅行偏好']);
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
    expect(routePaths).toContain('/inspirations/:id');
    expect(routePaths).toContain('/admin/resources/users');
    expect(routePaths).toContain('/admin/settings');
  });

  it('creates an API client with the configured base URL', () => {
    const client = createHttpClient({ baseURL: 'http://localhost:8080/api' });
    expect(client.defaults.baseURL).toBe('http://localhost:8080/api');
    expect(client.defaults.timeout).toBe(10000);
  });

  it('does not serialize file uploads as JSON', async () => {
    const client = createHttpClient();
    let request;
    client.defaults.adapter = async (config) => {
      request = config;
      return { data: {}, status: 200, statusText: 'OK', headers: {}, config };
    };
    const form = new FormData();
    form.append('image', new Blob(['image'], { type: 'image/png' }), 'image.png');

    await client.post('/user/uploads/images', form);

    expect(request.data).toBe(form);
    expect(request.headers.getContentType()).not.toBe('application/json');
  });
});
