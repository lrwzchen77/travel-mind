import { describe, expect, it, vi } from 'vitest';
import { createMemoryApi, memoryImageUrl } from './memory.js';

describe('memory API', () => {
  it('uses owned memory routes and chains real uploads before association', async () => {
    const client = {
      get: vi.fn().mockResolvedValue({ data: { data: {} } }),
      post: vi.fn().mockResolvedValue({ data: { data: { id: 31 } } }),
      delete: vi.fn().mockResolvedValue({ data: { data: null } }),
    };
    const uploader = { image: vi.fn()
      .mockResolvedValueOnce({ url: '/uploads/one.jpg' })
      .mockResolvedValueOnce({ url: '/uploads/two.jpg' }) };
    const api = createMemoryApi(client, uploader);
    const files = [{ name: 'one.jpg' }, { name: 'two.jpg' }];

    await api.createFromTrip(9001);
    await api.addPhotos(3001, files);
    await api.ask(3001, '去了哪里？');
    await api.publish(3001, { title: '杭州回忆', photo_item_id: 31 });

    expect(client.post).toHaveBeenCalledWith('/user/trips/9001/memory');
    expect(uploader.image).toHaveBeenNthCalledWith(1, files[0]);
    expect(client.post).toHaveBeenCalledWith('/user/memories/3001/items/photos', { url: '/uploads/one.jpg' });
    expect(client.post).toHaveBeenCalledWith('/user/memories/3001/items/photos', { url: '/uploads/two.jpg' });
    expect(client.post).toHaveBeenCalledWith('/user/memories/3001/ask', { question: '去了哪里？' });
    expect(client.post).toHaveBeenCalledWith('/user/memories/3001/publish', { title: '杭州回忆', photo_item_id: 31 });
  });

  it('keeps non-upload URLs untouched and resolves private upload paths through the API host', () => {
    expect(memoryImageUrl('https://example.com/a.jpg')).toBe('https://example.com/a.jpg');
    expect(memoryImageUrl('/uploads/a.jpg')).toBe('http://localhost:8080/uploads/a.jpg');
  });

  it('reports how many photos were associated before a partial batch failure', async () => {
    const failure = new Error('second upload failed');
    const client = { post: vi.fn().mockResolvedValue({ data: { data: { id: 31 } } }) };
    const uploader = { image: vi.fn().mockResolvedValueOnce({ url: '/uploads/one.jpg' }).mockRejectedValueOnce(failure) };
    const api = createMemoryApi(client, uploader);

    await expect(api.addPhotos(3001, [{ name: 'one.jpg' }, { name: 'two.jpg' }])).rejects.toMatchObject({ addedCount: 1 });
    expect(client.post).toHaveBeenCalledTimes(1);
  });
});
