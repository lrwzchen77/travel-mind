import { beforeEach, describe, expect, it, vi } from 'vitest';
import { authSession } from '../auth/session.js';
import { useFavorites } from './useFavorites.js';

describe('useFavorites', () => {
  beforeEach(() => {
    authSession.clear();
    authSession.save({ tokenValue: 'token', user: { roles: ['user'] } });
  });

  it('loads, removes, and recreates favorite state without duplicate records', async () => {
    const api = {
      userList: vi.fn().mockResolvedValue({
        records: [
          { id: 10, target_type: 'city', target_id: 100, note: '杭州' },
          { id: 12, target_type: 'city', target_id: 100, note: '杭州' },
        ],
      }),
      userRemove: vi.fn().mockResolvedValue({}),
      userCreate: vi.fn().mockResolvedValue({ id: 11 }),
    };
    const favorites = useFavorites(api);

    await favorites.loadFavorites();
    expect(favorites.isFavorite('city', 100)).toBe(true);

    await favorites.toggleFavorite({ targetType: 'city', targetId: 100, note: '杭州' });
    expect(api.userRemove).toHaveBeenCalledWith('favorites', 10);
    expect(api.userRemove).toHaveBeenCalledWith('favorites', 12);
    expect(favorites.isFavorite('city', 100)).toBe(false);

    await favorites.toggleFavorite({ targetType: 'city', targetId: 100, note: '杭州' });
    expect(api.userCreate).toHaveBeenCalledTimes(1);
    expect(favorites.isFavorite('city', 100)).toBe(true);
  });
});
