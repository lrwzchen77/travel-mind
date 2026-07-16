import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import CommunityView from './CommunityView.vue';

const mocks = vi.hoisted(() => ({
  createPost: vi.fn(),
  isLoggedIn: vi.fn(),
  posts: vi.fn(),
  push: vi.fn(),
  route: { query: { city: '杭州' }, fullPath: '/inspirations?city=杭州' },
  upload: vi.fn(),
}));

vi.mock('../api/community.js', () => ({
  communityApi: { createPost: mocks.createPost, posts: mocks.posts },
}));
vi.mock('../api/upload.js', () => ({ uploadApi: { image: mocks.upload } }));
vi.mock('../auth/session.js', () => ({
  authSession: { isLoggedIn: mocks.isLoggedIn },
}));
vi.mock('vue-router', () => ({
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
  useRoute: () => mocks.route,
  useRouter: () => ({ push: mocks.push }),
}));

const firstPost = {
  id: 7,
  title: '西湖边的慢行路线',
  city: '杭州',
  topic: 'route',
  author: '阿青',
  content: '从曲院风荷散步到孤山。',
  tags: '少走路,看日落',
};

function mountPage() {
  return mount(CommunityView);
}

async function openComposer(wrapper) {
  mocks.isLoggedIn.mockReturnValue(true);
  await wrapper.find('.community-intro button').trigger('click');
}

beforeEach(() => {
  vi.clearAllMocks();
  mocks.route.query = { city: '杭州' };
  mocks.route.fullPath = '/inspirations?city=杭州';
  mocks.isLoggedIn.mockReturnValue(false);
  mocks.posts.mockResolvedValue({ records: [firstPost], total: 1 });
  mocks.createPost.mockResolvedValue({});
  mocks.upload.mockResolvedValue({ url: '/uploads/cover.png' });
});

describe('旅行社区页', () => {
  it('按路由城市加载列表，并携带完整筛选参数重新查询', async () => {
    const wrapper = mountPage();
    await flushPromises();

    expect(mocks.posts).toHaveBeenCalledWith({ keyword: '', city: '杭州', topic: '', pageSize: 24 });
    expect(wrapper.text()).toContain('西湖边的慢行路线');
    expect(wrapper.text()).toContain('1 篇旅行者分享');

    const filter = wrapper.find('.community-filter');
    const [keyword, city] = filter.findAll('input');
    await keyword.setValue('日落');
    await city.setValue('苏州');
    await filter.find('select').setValue('play');
    await filter.trigger('submit');
    await flushPromises();

    expect(mocks.posts).toHaveBeenLastCalledWith({ keyword: '日落', city: '苏州', topic: 'play', pageSize: 24 });
  });

  it('未登录时带当前地址跳转登录页', async () => {
    const wrapper = mountPage();
    await flushPromises();

    await wrapper.find('.community-intro button').trigger('click');

    expect(mocks.push).toHaveBeenCalledWith({ path: '/login', query: { redirect: '/inspirations?city=杭州' } });
    expect(wrapper.find('.community-compose').exists()).toBe(false);
  });

  it('已登录时可展开和收起发布表单', async () => {
    const wrapper = mountPage();
    await flushPromises();

    await openComposer(wrapper);
    expect(wrapper.find('.community-compose').exists()).toBe(true);
    expect(wrapper.find('.community-intro button').text()).toBe('收起发布');

    await wrapper.find('.community-intro button').trigger('click');
    expect(wrapper.find('.community-compose').exists()).toBe(false);
  });

  it('回写上传封面并提交完整数据，成功后重置表单和刷新列表', async () => {
    let submitted;
    mocks.createPost.mockImplementation((payload) => {
      submitted = { ...payload };
      return Promise.resolve({ id: 8 });
    });
    const wrapper = mountPage();
    await flushPromises();
    await openComposer(wrapper);
    const composer = wrapper.find('.community-compose');

    await composer.find('input[placeholder^="例如：杭州两天"]').setValue('雨天的杭州');
    await composer.find('input[placeholder="例如：杭州"]').setValue('绍兴');
    await composer.find('input[placeholder="亲子、少走路、美食"]').setValue('雨天,老街');
    await composer.findAll('select')[0].setValue('tip');
    await composer.findAll('select')[1].setValue('private');
    await composer.find('textarea').setValue('安排室内展馆，晚上再逛老街。');

    const file = new File(['png'], 'cover.png', { type: 'image/png' });
    const fileInput = composer.find('input[type="file"]');
    Object.defineProperty(fileInput.element, 'files', { value: [file], configurable: true });
    await fileInput.trigger('change');
    await flushPromises();

    expect(mocks.upload).toHaveBeenCalledWith(file);
    expect(composer.find('img[alt="已上传图片预览"]').attributes('src')).toBe('/uploads/cover.png');

    await composer.trigger('submit');
    await flushPromises();

    expect(submitted).toEqual({
      title: '雨天的杭州', city: '绍兴', topic: 'tip', tags: '雨天,老街',
      cover_image: '/uploads/cover.png', content: '安排室内展馆，晚上再逛老街。', visibility: 'private',
    });
    expect(mocks.posts).toHaveBeenCalledTimes(2);
    expect(wrapper.find('.community-compose').exists()).toBe(false);
    expect(wrapper.text()).toContain('已提交发布；审核通过后会出现在旅行社区。');

    await openComposer(wrapper);
    const resetComposer = wrapper.find('.community-compose');
    expect(resetComposer.find('input[placeholder^="例如：杭州两天"]').element.value).toBe('');
    expect(resetComposer.find('input[placeholder="例如：杭州"]').element.value).toBe('杭州');
    expect(resetComposer.find('textarea').element.value).toBe('');
    expect(resetComposer.find('img[alt="已上传图片预览"]').exists()).toBe(false);
  });

  it('展示列表加载和发布失败的错误', async () => {
    mocks.posts.mockRejectedValueOnce(new Error('社区服务不可用'));
    const wrapper = mountPage();
    await flushPromises();
    expect(wrapper.find('.error-line').text()).toBe('社区服务不可用');

    mocks.createPost.mockRejectedValueOnce(new Error('内容未通过校验'));
    await openComposer(wrapper);
    await wrapper.find('.community-compose').trigger('submit');
    await flushPromises();

    expect(wrapper.find('.error-line').text()).toBe('内容未通过校验');
    expect(wrapper.find('.community-compose').exists()).toBe(true);
  });
});
