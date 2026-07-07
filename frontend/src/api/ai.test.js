import { describe, expect, it, vi } from 'vitest';
import { createAiApi } from './ai.js';

describe('AI API client', () => {
  it('calls vision, trip evaluation, content analysis, and comfort endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: { data: {} } }),
      post: vi.fn().mockResolvedValue({ data: { data: {} } }),
    };
    const api = createAiApi(http);

    await api.detectVision({ image_url: 'https://example.com/a.jpg' });
    await api.evaluateTrip({ days: [] }, { targetId: 9001 });
    await api.analyzeContent({ text: '西湖很好' }, { targetId: 7001 });
    await api.tripComfort(9001);

    expect(http.post).toHaveBeenCalledWith('/ai/vision/detect', { image_url: 'https://example.com/a.jpg' });
    expect(http.post).toHaveBeenCalledWith('/ai/trip/evaluate', { days: [] }, { params: { targetId: 9001 } });
    expect(http.post).toHaveBeenCalledWith('/ai/content/analyze', { text: '西湖很好' }, { params: { targetId: 7001 } });
    expect(http.get).toHaveBeenCalledWith('/ai/trip/9001/comfort');
  });
});
