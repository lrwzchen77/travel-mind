import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import InspirationDetailView from './InspirationDetailView.vue';

const mocks = vi.hoisted(() => ({
  comments: vi.fn(), createComment: vi.fn(), deleteComment: vi.fn(), isLoggedIn: vi.fn(),
  like: vi.fn(), post: vi.fn(), push: vi.fn(), unlike: vi.fn(),
  route: { params: { id: '7001' }, fullPath: '/inspirations/7001' },
}));

vi.mock('../api/community.js', () => ({ communityApi: {
  comments: mocks.comments, createComment: mocks.createComment, deleteComment: mocks.deleteComment,
  like: mocks.like, post: mocks.post, unlike: mocks.unlike, addToBag: vi.fn(),
} }));
vi.mock('../auth/session.js', () => ({ authSession: { isLoggedIn: mocks.isLoggedIn } }));
vi.mock('vue-router', () => ({
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
  useRoute: () => mocks.route,
  useRouter: () => ({ push: mocks.push }),
}));

beforeEach(() => {
  vi.clearAllMocks();
  mocks.isLoggedIn.mockReturnValue(false);
  mocks.post.mockResolvedValue({ id: 7001, title: '西湖慢游', content: '沿湖散步', like_count: 2, comment_count: 1, liked_by_me: false });
  mocks.comments.mockResolvedValue({ records: [{ id: 9, author: '阿青', content: '傍晚更舒服', is_mine: false }] });
  mocks.like.mockResolvedValue({ like_count: 3, liked_by_me: true });
  mocks.createComment.mockResolvedValue({ id: 10 });
});

describe('社区分享互动', () => {
  it('公开加载计数和评论，匿名点赞带当前地址登录', async () => {
    const wrapper = mount(InspirationDetailView);
    await flushPromises();

    expect(mocks.post).toHaveBeenCalledWith('7001');
    expect(mocks.comments).toHaveBeenCalledWith('7001', { pageSize: 50 });
    expect(wrapper.text()).toContain('点赞 · 2');
    expect(wrapper.text()).toContain('傍晚更舒服');

    await wrapper.find('.like-button').trigger('click');
    expect(mocks.push).toHaveBeenCalledWith({ path: '/login', query: { redirect: '/inspirations/7001' } });
    expect(mocks.like).not.toHaveBeenCalled();
  });

  it('登录用户可点赞、发评论并只看到本人评论的删除按钮', async () => {
    mocks.isLoggedIn.mockReturnValue(true);
    mocks.comments
      .mockResolvedValueOnce({ records: [{ id: 9, author: '阿青', content: '傍晚更舒服', is_mine: false }] })
      .mockResolvedValueOnce({ records: [{ id: 10, author: '我', content: '补充公交信息', is_mine: true }] });
    const wrapper = mount(InspirationDetailView);
    await flushPromises();

    await wrapper.find('.like-button').trigger('click');
    await flushPromises();
    expect(mocks.like).toHaveBeenCalledWith(7001);
    expect(wrapper.find('.like-button').attributes('aria-pressed')).toBe('true');

    await wrapper.find('#comment').setValue('补充公交信息');
    await wrapper.find('.comment-form').trigger('submit');
    await flushPromises();
    expect(mocks.createComment).toHaveBeenCalledWith(7001, '补充公交信息');
    expect(wrapper.find('.comment-list button').text()).toBe('删除');
  });
});
