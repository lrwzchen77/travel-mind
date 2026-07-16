import { http } from './http.js';

export const uploadApi = {
  image(file) {
    const data = new FormData();
    data.append('image', file);
    return http.post('/user/uploads/images', data)
      .then((response) => response.data?.data ?? response.data);
  },
};
