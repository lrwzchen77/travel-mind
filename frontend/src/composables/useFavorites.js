import { ref } from 'vue';
import { resourceApi } from '../api/resources.js';
import { authSession } from '../auth/session.js';

function favoriteKey(targetType, targetId) {
  return `${targetType}:${targetId}`;
}

export function useFavorites(api = resourceApi) {
  const records = ref(new Map());
  const busyKey = ref('');

  function isFavorite(targetType, targetId) {
    return records.value.has(favoriteKey(targetType, targetId));
  }

  async function loadFavorites() {
    if (!authSession.isLoggedIn()) return;
    const data = await api.userList('favorites', { pageSize: 200 });
    const next = new Map();
    for (const item of data.records || []) {
      const key = favoriteKey(item.target_type, item.target_id);
      const current = next.get(key);
      next.set(key, {
        ...item,
        favorite_ids: [...(current?.favorite_ids || []), item.id],
      });
    }
    records.value = next;
  }

  async function toggleFavorite({ targetType, targetId, note }) {
    if (!authSession.isLoggedIn()) return { requiresLogin: true };
    const key = favoriteKey(targetType, targetId);
    busyKey.value = key;
    try {
      const current = records.value.get(key);
      const next = new Map(records.value);
      if (current) {
        await Promise.all(
          (current.favorite_ids || [current.id]).map((id) => api.userRemove('favorites', id)),
        );
        next.delete(key);
        records.value = next;
        return { favorite: false };
      }
      const created = await api.userCreate('favorites', {
        target_type: targetType,
        target_id: targetId,
        note,
      });
      next.set(key, { ...created, target_type: targetType, target_id: targetId, note });
      records.value = next;
      return { favorite: true };
    } finally {
      busyKey.value = '';
    }
  }

  return { busyKey, isFavorite, loadFavorites, toggleFavorite };
}
