import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ResourceCrudView from './ResourceCrudView.vue';

const mocks = vi.hoisted(() => ({ list: vi.fn() }));
vi.mock('../api/resources.js', () => ({ resourceApi: {
  list: mocks.list, create: vi.fn(), update: vi.fn(), updateStatus: vi.fn(), remove: vi.fn(), resetPassword: vi.fn(), updateRole: vi.fn(),
} }));
vi.mock('../api/ai.js', () => ({ adminAiApi: { analyzeContent: vi.fn() } }));
vi.mock('../api/community.js', () => ({ communityApi: { reviewPost: vi.fn() } }));
vi.mock('../components/PagePrologue.vue', () => ({ default: { template: '<header />' } }));
vi.mock('vue-router', () => ({
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
  useRoute: () => ({ fullPath: '/admin/resources/attractions', meta: {
    resourceKey: 'attractions', title: '景点内容', fields: ['id', 'name', 'status'], fieldLabels: {}, admin: true,
  } }),
}));

describe('管理资源分页', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.list.mockImplementation((key, params) => Promise.resolve({
      records: [{ id: params.pageNum, name: `景点 ${params.pageNum}`, status: 1 }], total: 25,
    }));
  });

  it('pages through backend records and uses the backend image field name', async () => {
    const wrapper = mount(ResourceCrudView);
    await flushPromises();
    expect(mocks.list).toHaveBeenCalledWith('attractions', expect.objectContaining({ pageNum: 1, pageSize: 20 }));
    await wrapper.findAll('.admin-pagination button')[1].trigger('click');
    await flushPromises();
    expect(mocks.list).toHaveBeenLastCalledWith('attractions', expect.objectContaining({ pageNum: 2, pageSize: 20 }));
    await wrapper.get('.toolbar .btn-ghost').trigger('click');
    expect(wrapper.get('.code-area').element.value).toContain('"image_url"');
    expect(wrapper.get('.code-area').element.value).not.toContain('"imageUrl"');
  });
});
