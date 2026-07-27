import { describe, expect, it, vi } from 'vitest';
import { createAiApi } from './ai.js';

describe('AI API client', () => {
  it('exposes only user-facing analysis and comfort endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: { data: {} } }),
      post: vi.fn().mockResolvedValue({ data: { data: {} } }),
    };
    const api = createAiApi(http);

    await api.detectVision({ image_url: 'https://example.com/a.jpg' });
    await api.analyzeContent({ text: '西湖很好' }, { targetId: 7001 });
    await api.tripComfort(9001);
    await api.tripComfortFeedback(9001);
    await api.saveTripComfortFeedback(9001, { actual_label: 'balanced' });

    expect(http.post).toHaveBeenCalledWith('/user/ai/vision/detect', { image_url: 'https://example.com/a.jpg' });
    expect(http.post).toHaveBeenCalledWith('/user/ai/content/analyze', { text: '西湖很好' }, { params: { targetId: 7001 } });
    expect(http.get).toHaveBeenCalledWith('/user/ai/trip/9001/comfort');
    expect(http.get).toHaveBeenCalledWith('/user/ai/trip/9001/comfort/feedback');
    expect(http.post).toHaveBeenCalledWith('/user/ai/trip/9001/comfort/feedback', { actual_label: 'balanced' });
    expect(api.evaluateTrip).toBeUndefined();
    expect(api.comfortFeedbackStats).toBeUndefined();
  });

  it('supports the isolated admin AI prefix', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: { data: {} } }),
      post: vi.fn().mockResolvedValue({ data: { data: {} } }),
    };
    const api = createAiApi(http, 'admin');
    await api.detectVision({ image_url: 'https://example.com/a.jpg' });
    await api.evaluateTrip({ days: [] }, { targetId: 9001 });
    await api.comfortFeedbackStats();
    expect(http.post).toHaveBeenCalledWith('/admin/ai/vision/detect', { image_url: 'https://example.com/a.jpg' });
    expect(http.post).toHaveBeenCalledWith('/admin/ai/trip/evaluate', { days: [] }, { params: { targetId: 9001 } });
    expect(http.get).toHaveBeenCalledWith('/admin/ai/travel-comfort/feedback/stats');
    expect(api.tripComfort).toBeUndefined();
  });
});
