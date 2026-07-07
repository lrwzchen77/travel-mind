import { describe, expect, it, vi } from 'vitest';
import { createTripApi } from './trip.js';

describe('trip API client', () => {
  it('calls planning, status, detail, copy, delete, chat, and history endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: { data: {} } }),
      post: vi.fn().mockResolvedValue({ data: { data: {} } }),
      delete: vi.fn().mockResolvedValue({ data: { deleted: true } }),
    };
    const api = createTripApi(http);
    const payload = { city: 'Hangzhou', travel_days: 2 };

    await api.submitPlan(payload);
    await api.status('task1');
    await api.detail(9001);
    await api.copy(9001);
    await api.remove(9001);
    await api.chat(9001, '预算是多少？');
    await api.history(10);

    expect(http.post).toHaveBeenCalledWith('/trip/plan', payload);
    expect(http.get).toHaveBeenCalledWith('/trip/status/task1');
    expect(http.get).toHaveBeenCalledWith('/trip/9001');
    expect(http.post).toHaveBeenCalledWith('/trip/9001/copy');
    expect(http.delete).toHaveBeenCalledWith('/trip/9001');
    expect(http.post).toHaveBeenCalledWith('/trip/9001/chat', { message: '预算是多少？', history: [] });
    expect(http.get).toHaveBeenCalledWith('/trip/history', { params: { limit: 10 } });
  });
});
