import { describe, expect, it, vi } from 'vitest';
import { createResourceApi } from './resources.js';

describe('resource API client', () => {
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

    expect(http.get).toHaveBeenCalledWith('/attractions', {
      params: { keyword: 'lake', cityId: 2001 },
    });
    expect(http.post).toHaveBeenCalledWith('/cities', { name: 'Suzhou' });
    expect(http.put).toHaveBeenCalledWith('/cities/2004', { name: 'Suzhou City' });
    expect(http.put).toHaveBeenCalledWith('/cities/2004/status', null, { params: { status: 0 } });
    expect(http.delete).toHaveBeenCalledWith('/cities/2004');
  });

  it('loads user profile and trip history from dedicated endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: { data: {} } }),
      put: vi.fn().mockResolvedValue({ data: { data: {} } }),
    };
    const api = createResourceApi(http);

    await api.getProfile(1001);
    await api.updateProfile(1001, { user: { nickname: 'Demo' } });
    await api.tripHistory(8);

    expect(http.get).toHaveBeenCalledWith('/users/profile', { params: { userId: 1001 } });
    expect(http.put).toHaveBeenCalledWith('/users/profile', { user: { nickname: 'Demo' } }, {
      params: { userId: 1001 },
    });
    expect(http.get).toHaveBeenCalledWith('/trip/history', { params: { limit: 8 } });
  });
});
