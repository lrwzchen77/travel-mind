import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MyPostsView from './MyPostsView.vue';

const myPosts = vi.hoisted(() => vi.fn());
vi.mock('../api/community.js', () => ({ communityApi: { myPosts } }));
vi.mock('vue-router', () => ({ RouterLink: { props: ['to'], template: '<a :data-to="to"><slot /></a>' } }));

describe('我的分享页', () => {
  beforeEach(() => {
    myPosts.mockResolvedValue({ records: [
      { id: 1, title: '西湖慢游', visibility: 'public', status: 1, topic: 'route', content: '一路慢慢走。' },
      { id: 2, title: '雨天避坑', visibility: 'public', status: 0, topic: 'tip', content: '雨伞别忘。' },
      { id: 3, title: '私藏早餐', visibility: 'private', status: 1, topic: 'food', content: '只留给自己。' },
    ] });
  });

  it('shows owned posts and their publish states', async () => {
    const wrapper = mount(MyPostsView);
    await flushPromises();
    expect(myPosts).toHaveBeenCalledWith({ pageSize: 30 });
    expect(wrapper.text()).toContain('1篇已发布');
    expect(wrapper.text()).toContain('1篇审核中');
    expect(wrapper.text()).toContain('仅自己可见');
    expect(wrapper.find('[data-to="/inspirations/1"]').exists()).toBe(true);
    expect(wrapper.find('[data-to="/inspirations/2"]').exists()).toBe(false);
    expect(wrapper.find('[data-to="/inspirations/3"]').exists()).toBe(false);
  });

  it('keeps an actionable error state when owned posts cannot load', async () => {
    myPosts.mockRejectedValueOnce(new Error('网络断开'));
    const wrapper = mount(MyPostsView);
    await flushPromises();
    expect(wrapper.find('.error-line').text()).toBe('网络断开');
    expect(wrapper.find('[data-to="/inspirations"]').exists()).toBe(true);
  });

  it('offers a clear first-publish action for an empty account', async () => {
    myPosts.mockResolvedValueOnce({ records: [] });
    const wrapper = mount(MyPostsView);
    await flushPromises();
    expect(wrapper.text()).toContain('还没有公开或私藏的旅行分享');
    expect(wrapper.find('[data-to="/inspirations"]').exists()).toBe(true);
  });
});
