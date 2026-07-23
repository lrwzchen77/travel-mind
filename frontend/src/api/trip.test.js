import { describe, expect, it, vi } from 'vitest';
import { createTripApi } from './trip.js';

describe('trip API client', () => {
  it('preserves the business data field in a trip detail response', async () => {
    const detail = {
      success: true,
      plan_id: '9001',
      data: { city: 'Hangzhou', days: [{ date: '2026-08-01' }] },
      graph_data: { nodes: [{ id: 'city-Hangzhou' }], edges: [] },
    };
    const http = { get: vi.fn().mockResolvedValue({ data: detail }) };

    await expect(createTripApi(http).detail(9001)).resolves.toEqual(detail);
  });

  it('calls planning, status, detail, copy, delete, chat, and history endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: { data: {} } }),
      post: vi.fn().mockResolvedValue({ data: { data: {} } }),
      delete: vi.fn().mockResolvedValue({ data: { deleted: true } }),
    };
    const api = createTripApi(http);
    const payload = { city: 'Hangzhou', travel_days: 2 };

    await api.submitPlan(payload);
    await api.publicMap('杭州', 120.1551, 30.2741);
    await api.status('task1');
    await api.detail(9001);
    await api.copy(9001);
    await api.remove(9001);
    await api.chat(9001, '预算是多少？');
    await api.expenses(9001);
    await api.addExpense(9001, { category: 'food', amount: 88 });
    await api.removeExpense(9001, 7001);
    await api.history(10);

    expect(http.post).toHaveBeenCalledWith('/user/trip/plan', payload);
    expect(http.get).toHaveBeenCalledWith('/public/travel-map', {
      params: { city: '杭州', longitude: 120.1551, latitude: 30.2741 }, timeout: 20000,
    });
    expect(http.get).toHaveBeenCalledWith('/user/trip/status/task1');
    expect(http.get).toHaveBeenCalledWith('/user/trip/9001');
    expect(http.post).toHaveBeenCalledWith('/user/trip/9001/copy');
    expect(http.delete).toHaveBeenCalledWith('/user/trip/9001');
    expect(http.post).toHaveBeenCalledWith('/user/trip/9001/chat', { message: '预算是多少？', history: [] });
    expect(http.get).toHaveBeenCalledWith('/user/trip/9001/expenses');
    expect(http.post).toHaveBeenCalledWith('/user/trip/9001/expenses', { category: 'food', amount: 88 });
    expect(http.delete).toHaveBeenCalledWith('/user/trip/9001/expenses/7001');
    expect(http.get).toHaveBeenCalledWith('/user/trip/history', { params: { limit: 10 } });
  });
});
