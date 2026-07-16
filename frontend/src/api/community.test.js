import { describe, expect, it, vi } from 'vitest';
import { createCommunityApi } from './community.js';

describe('community API', () => {
  it('separates public inspiration browsing from owned bag changes', async () => {
    const http = { get: vi.fn().mockResolvedValue({ data: { data: { records: [] } } }), post: vi.fn().mockResolvedValue({ data: { data: {} } }), delete: vi.fn().mockResolvedValue({ data: { data: null } }) };
    const api = createCommunityApi(http);
    await api.posts({ city: '杭州' });
    await api.myPosts({ pageSize: 30 });
    await api.addToBag(7001, 'must');
    await api.removeFromBag(7001);
    expect(http.get).toHaveBeenCalledWith('/public/inspirations', { params: { city: '杭州' } });
    expect(http.get).toHaveBeenCalledWith('/user/inspirations/posts', { params: { pageSize: 30 } });
    expect(http.post).toHaveBeenCalledWith('/user/inspirations/bag', { post_id: 7001, intent: 'must' });
    expect(http.delete).toHaveBeenCalledWith('/user/inspirations/bag/7001');
  });
});
