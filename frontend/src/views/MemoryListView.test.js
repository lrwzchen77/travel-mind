import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MemoryListView from './MemoryListView.vue';

const mocks = vi.hoisted(() => ({ list: vi.fn() }));

vi.mock('../api/memory.js', () => ({
  memoryApi: { list: mocks.list },
  memoryImageUrl: (path) => path,
}));
vi.mock('vue-router', () => ({
  RouterLink: { props: ['to'], template: '<a :data-to="to"><slot /></a>' },
}));

beforeEach(() => {
  vi.clearAllMocks();
});

describe('旅行记录列表', () => {
  it('shows only trip-facing card information and one actionable reminder', async () => {
    mocks.list.mockResolvedValue({ records: [{
      id: '996889308694955191', destination_city: '杭州', title: '杭州两日游', summary: '慢游西湖',
      cover_image: '/uploads/cover.jpg', update_time: '2026-08-03T10:00:00', item_count: 8,
      generation_status: 'ready', index_status: 'pending',
    }] });
    const wrapper = mount(MemoryListView);
    await flushPromises();

    expect(wrapper.text()).toContain('旅行记录');
    expect(wrapper.text()).toContain('杭州 · 8月3日更新');
    expect(wrapper.text()).toContain('打开后可以准备查找旅行细节');
    expect(wrapper.text()).not.toMatch(/8 条|整理状态|索引|问答|证据/);
    expect(wrapper.get('.memory-card-cover em').text()).toBe('私密');
  });

  it('invites an empty account to start from a real existing trip', async () => {
    mocks.list.mockResolvedValue({ records: [] });
    const wrapper = mount(MemoryListView);
    await flushPromises();

    expect(wrapper.text()).toContain('还没有旅行记录');
    expect(wrapper.text()).toContain('从一趟已有行程开始');
    expect(wrapper.get('.memory-empty a').attributes('data-to')).toBe('/trip-history');
  });
});
