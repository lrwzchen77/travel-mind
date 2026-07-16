import { http } from './http.js';

function unwrap(response) {
  return response.data?.data ?? response.data;
}

function notifyBagChanged() {
  globalThis.dispatchEvent?.(new Event('inspiration-bag-changed'));
}

export function createCommunityApi(client = http) {
  return {
    posts(params = {}) { return client.get('/public/inspirations', { params }).then(unwrap); },
    post(id) { return client.get(`/public/inspirations/${id}`).then(unwrap); },
    myPosts(params = {}) { return client.get('/user/inspirations/posts', { params }).then(unwrap); },
    createPost(payload) { return client.post('/user/inspirations/posts', payload).then(unwrap); },
    bag() { return client.get('/user/inspirations/bag').then(unwrap); },
    addToBag(post_id, intent = 'reference') {
      return client.post('/user/inspirations/bag', { post_id, intent }).then(unwrap).then((data) => {
        notifyBagChanged();
        return data;
      });
    },
    removeFromBag(postId) {
      return client.delete(`/user/inspirations/bag/${postId}`).then(unwrap).then((data) => {
        notifyBagChanged();
        return data;
      });
    },
  };
}

export const communityApi = createCommunityApi();
