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
    await api.comments(7001, { pageSize: 50 });
    await api.like(7001);
    await api.unlike(7001);
    await api.createComment(7001, '路线很实用');
    await api.deleteComment(9001);
    expect(http.get).toHaveBeenCalledWith('/public/inspirations', { params: { city: '杭州' } });
    expect(http.get).toHaveBeenCalledWith('/user/inspirations/posts', { params: { pageSize: 30 } });
    expect(http.post).toHaveBeenCalledWith('/user/inspirations/bag', { post_id: 7001, intent: 'must' });
    expect(http.delete).toHaveBeenCalledWith('/user/inspirations/bag/7001');
    expect(http.get).toHaveBeenCalledWith('/public/inspirations/7001/comments', { params: { pageSize: 50 } });
    expect(http.post).toHaveBeenCalledWith('/user/inspirations/7001/likes');
    expect(http.delete).toHaveBeenCalledWith('/user/inspirations/7001/likes');
    expect(http.post).toHaveBeenCalledWith('/user/inspirations/7001/comments', { content: '路线很实用' });
    expect(http.delete).toHaveBeenCalledWith('/user/inspirations/comments/9001');
  });
});
