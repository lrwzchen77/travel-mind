import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import UserLibraryView from './UserLibraryView.vue';

const mocks = vi.hoisted(() => ({ userList: vi.fn(), updateNote: vi.fn() }));
vi.mock('../api/resources.js', () => ({ resourceApi: {
  userList: mocks.userList, updateNote: mocks.updateNote, userCreate: vi.fn(), userRemove: vi.fn(),
} }));
vi.mock('vue-router', () => ({
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
  useRoute: () => ({ meta: { resourceKey: 'travel-notes', title: '旅行笔记' } }),
}));
vi.mock('../components/PagePrologue.vue', () => ({ default: { template: '<header />' } }));

describe('旅行笔记编辑', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.userList.mockResolvedValue({ records: [{ id: 9, title: '西湖慢游', content: '沿湖散步', visibility: 'private' }], total: 1 });
    mocks.updateNote.mockResolvedValue({ id: 9 });
  });

  it('edits an owned note through the user library endpoint', async () => {
    const wrapper = mount(UserLibraryView);
    await flushPromises();
    await wrapper.get('.library-edit').trigger('click');
    await wrapper.get('input[aria-label="笔记标题"]').setValue('西湖慢游更新版');
    await wrapper.get('.library-edit-form').trigger('submit');
    await flushPromises();

    expect(mocks.updateNote).toHaveBeenCalledWith(9, { title: '西湖慢游更新版', content: '沿湖散步' });
    expect(wrapper.text()).toContain('笔记修改已保存');
  });

  it('searches the current library without accepting a user id', async () => {
    const wrapper = mount(UserLibraryView);
    await flushPromises();
    await wrapper.get('#library-keyword').setValue('西湖');
    await wrapper.get('.library-search').trigger('submit');
    await flushPromises();
    expect(mocks.userList).toHaveBeenLastCalledWith('travel-notes', { keyword: '西湖', pageNum: 1, pageSize: 30 });
  });
});
