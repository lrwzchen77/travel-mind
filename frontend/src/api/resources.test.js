import { describe, expect, it, vi } from 'vitest';
import { createResourceApi } from './resources.js';

describe('resource API client', () => {
  it('separates public discovery and current-user library endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: { data: { records: [] } } }),
      post: vi.fn().mockResolvedValue({ data: { data: { id: 1 } } }),
      put: vi.fn().mockResolvedValue({ data: { data: { id: 1 } } }),
    };
    const api = createResourceApi(http);

    await api.discover('cities', { keyword: '杭州' });
    await api.discoverDetail('attractions', 7);
    await api.userCreate('favorites', { target_type: 'city', target_id: 2001, note: '杭州' });
    await api.updateNote(9, { title: '西湖慢游', content: '沿湖散步' });

    expect(http.get).toHaveBeenCalledWith('/public/resources/cities', { params: { keyword: '杭州' } });
    expect(http.get).toHaveBeenCalledWith('/public/resources/attractions/7');
    expect(http.post).toHaveBeenCalledWith('/user/library/favorites', {
      target_type: 'city', target_id: 2001, note: '杭州',
    });
    expect(http.put).toHaveBeenCalledWith('/user/library/travel-notes/9', { title: '西湖慢游', content: '沿湖散步' });
  });

  it('calls backend CRUD endpoints with filters and payloads', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: { data: { records: [] } } }),
      post: vi.fn().mockResolvedValue({ data: { data: { id: 1 } } }),
      put: vi.fn().mockResolvedValue({ data: { data: { id: 1 } } }),
      delete: vi.fn().mockResolvedValue({ data: { code: 0 } }),
    };
    const api = createResourceApi(http);

    await api.list('attractions', { keyword: 'lake', cityId: 2001 });
    await api.create('cities', { name: 'Suzhou' });
    await api.update('cities', 2004, { name: 'Suzhou City' });
    await api.updateStatus('cities', 2004, 0);
    await api.remove('cities', 2004);

    expect(http.get).toHaveBeenCalledWith('/admin/resources/attractions', {
      params: { keyword: 'lake', cityId: 2001 },
    });
    expect(http.post).toHaveBeenCalledWith('/admin/resources/cities', { name: 'Suzhou' });
    expect(http.put).toHaveBeenCalledWith('/admin/resources/cities/2004', { name: 'Suzhou City' });
    expect(http.put).toHaveBeenCalledWith('/admin/resources/cities/2004/status', null, { params: { status: 0 } });
    expect(http.delete).toHaveBeenCalledWith('/admin/resources/cities/2004');
  });

  it('loads and updates the current user profile without accepting a user id', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: { data: {} } }),
      put: vi.fn().mockResolvedValue({ data: { data: {} } }),
    };
    const api = createResourceApi(http);

    await api.getProfile();
    await api.updateProfile({ user: { nickname: 'Demo' } });

    expect(http.get).toHaveBeenCalledWith('/user/profile');
    expect(http.put).toHaveBeenCalledWith('/user/profile', { user: { nickname: 'Demo' } });
  });
});
