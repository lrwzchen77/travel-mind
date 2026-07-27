import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MemoryDetailView from './MemoryDetailView.vue';

const mocks = vi.hoisted(() => ({
  addPhotos: vi.fn(), analyze: vi.fn(), ask: vi.fn(), detail: vi.fn(), index: vi.fn(),
  publish: vi.fn(), remove: vi.fn(), removeItem: vi.fn(), push: vi.fn(), replace: vi.fn(),
  route: { params: { id: '3001' }, path: '/memories/3001', query: {} },
}));

vi.mock('../api/memory.js', () => ({
  memoryApi: {
    addPhotos: mocks.addPhotos, analyze: mocks.analyze, ask: mocks.ask, detail: mocks.detail,
    index: mocks.index, publish: mocks.publish, remove: mocks.remove, removeItem: mocks.removeItem,
  },
  memoryImageUrl: (path) => path,
}));
vi.mock('vue-router', () => ({
  RouterLink: { props: ['to'], template: '<a><slot /></a>' },
  useRoute: () => mocks.route,
  useRouter: () => ({ push: mocks.push, replace: mocks.replace }),
}));

const memory = {
  id: '3001', title: '杭州两日游', destination_city: '杭州', summary: '慢游西湖', visibility: 'private',
  generation_status: 'ready', index_status: 'ready',
  items: [
    { id: '996889308694955191', item_type: 'place', source_type: 'trip_item', place_name: '西湖', content: '湖边慢游', day_index: 1, taken_at: '2026-08-01T09:00:00' },
    { id: '996889308694955192', item_type: 'expense', source_type: 'trip_expense', place_name: '龙井体验', content: 'food，¥188', day_index: 1 },
    { id: '996889308694955193', item_type: 'photo', source_url: '/private-uploads/1001/photo.jpg', place_name: '断桥', ai_caption: '断桥，湖景，照片', ai_tags: '["湖景"]', day_index: 1, taken_at: '2026-08-01T10:00:00' },
  ],
  generations: [],
};

function mountPage(options) {
  return mount(MemoryDetailView, options);
}

beforeEach(() => {
  vi.clearAllMocks();
  vi.restoreAllMocks();
  mocks.route.query = {};
  mocks.detail.mockResolvedValue(structuredClone(memory));
  mocks.addPhotos.mockResolvedValue([]);
  mocks.analyze.mockResolvedValue({ version: 3 });
  mocks.index.mockResolvedValue({ indexedItems: 3 });
  mocks.publish.mockResolvedValue({ id: 7001, status: 0 });
  mocks.remove.mockResolvedValue();
  mocks.removeItem.mockResolvedValue();
  mocks.replace.mockResolvedValue();
});

describe('旅行记录详情', () => {
  it('leads with the private trip and real date stamp without internal system terms', async () => {
    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('私密 · 仅你可见');
    expect(wrapper.get('.memory-date-stamp').text()).toContain('8月1日');
    expect(wrapper.get('.memory-date-stamp').text()).toContain('第1天');
    expect(wrapper.get('.memory-date-stamp').text()).toContain('西湖');
    expect(wrapper.text()).toContain('这项消费只有你能看到');
    expect(wrapper.text()).not.toMatch(/PRIVATE ARCHIVE|知识库|索引|检索|证据|fallback|脱敏副本/);
    expect(wrapper.find('.memory-ask').exists()).toBe(false);
  });

  it('uses readable no-date stamps for the trip overview and later days', async () => {
    mocks.detail.mockResolvedValue({
      ...structuredClone(memory),
      items: [
        { id: '996889308694955190', item_type: 'trip_summary', place_name: '杭州亲子周末深度旅行完整标题', content: '旅行概览' },
        { id: '996889308694955194', item_type: 'place', place_name: '灵隐寺', content: '寺院慢游', day_index: 2 },
      ],
    });
    const wrapper = mountPage();
    await flushPromises();
    const stamps = wrapper.findAll('.memory-date-stamp');

    expect(stamps[0].text()).toContain('旅行概览');
    expect(stamps[0].text()).toContain('时间待确认');
    expect(stamps[0].text()).toContain('杭州');
    expect(stamps[0].text()).not.toContain('杭州亲子周末深度旅行完整标题');
    expect(stamps[0].text().match(/时间待确认/g)).toHaveLength(1);
    expect(stamps[1].text()).toContain('第2天');
    expect(stamps[1].text()).toContain('时间待确认');
    expect(stamps[1].text()).toContain('灵隐寺');
    expect(stamps[1].text().match(/时间待确认/g)).toHaveLength(1);
  });

  it('consumes the create marker before one analyze-to-index update and does not update on a normal refresh', async () => {
    mocks.route.query = { update: '1', from: 'trip' };
    const wrapper = mountPage();
    await flushPromises();

    expect(mocks.replace).toHaveBeenCalledWith({ path: '/memories/3001', query: { from: 'trip' } });
    expect(mocks.replace.mock.invocationCallOrder[0]).toBeLessThan(mocks.analyze.mock.invocationCallOrder[0]);
    expect(mocks.analyze).toHaveBeenCalledTimes(1);
    expect(mocks.index).toHaveBeenCalledTimes(1);
    expect(mocks.analyze.mock.invocationCallOrder[0]).toBeLessThan(mocks.index.mock.invocationCallOrder[0]);
    wrapper.unmount();

    vi.clearAllMocks();
    mocks.route.query = {};
    mocks.detail.mockResolvedValue(structuredClone(memory));
    mountPage();
    await flushPromises();
    expect(mocks.analyze).not.toHaveBeenCalled();
    expect(mocks.index).not.toHaveBeenCalled();
  });

  it('updates manually when an unchanged page reports new content', async () => {
    mocks.detail.mockResolvedValue({ ...structuredClone(memory), generation_status: 'pending', index_status: 'pending' });
    const wrapper = mountPage();
    await flushPromises();
    expect(mocks.analyze).not.toHaveBeenCalled();

    await wrapper.get('.memory-update-state button').trigger('click');
    await flushPromises();
    expect(mocks.analyze).toHaveBeenCalledWith('3001');
    expect(mocks.index).toHaveBeenCalledWith('3001');
    expect(mocks.analyze.mock.invocationCallOrder[0]).toBeLessThan(mocks.index.mock.invocationCallOrder[0]);
  });

  it('prepares detail search without rearranging when the record content is already ready', async () => {
    mocks.detail.mockResolvedValue({ ...structuredClone(memory), generation_status: 'ready', index_status: 'pending' });
    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.find('.memory-update-state').exists()).toBe(false);
    expect(wrapper.text()).not.toContain('这本记录有新内容');
    await wrapper.get('[aria-controls="memory-ask-panel"]').trigger('click');
    expect(wrapper.get('.memory-find-notice').text()).toContain('记录内容已更新');
    await wrapper.get('.memory-find-notice button').trigger('click');
    await flushPromises();
    expect(mocks.index).toHaveBeenCalledWith('3001');
    expect(mocks.analyze).not.toHaveBeenCalled();
  });

  it('uploads a batch then automatically analyzes before preparing detail search', async () => {
    mocks.addPhotos.mockResolvedValue([{ id: '1' }, { id: '2' }]);
    const wrapper = mountPage();
    await flushPromises();
    const files = [new File(['one'], 'one.jpg', { type: 'image/jpeg' }), new File(['two'], 'two.png', { type: 'image/png' })];
    Object.defineProperty(wrapper.get('.memory-file-input').element, 'files', { value: files });
    await wrapper.get('.memory-file-input').trigger('change');
    await flushPromises();

    expect(mocks.addPhotos).toHaveBeenCalledWith('3001', files);
    expect(mocks.analyze).toHaveBeenCalledWith('3001');
    expect(mocks.index).toHaveBeenCalledWith('3001');
    expect(mocks.analyze.mock.invocationCallOrder[0]).toBeLessThan(mocks.index.mock.invocationCallOrder[0]);
  });

  it('keeps and arranges the photos associated before a partial batch failure', async () => {
    mocks.addPhotos.mockRejectedValue(Object.assign(new Error('second upload failed'), { addedCount: 1 }));
    const wrapper = mountPage();
    await flushPromises();
    const files = [new File(['one'], 'one.jpg', { type: 'image/jpeg' }), new File(['two'], 'two.jpg', { type: 'image/jpeg' })];
    Object.defineProperty(wrapper.get('.memory-file-input').element, 'files', { value: files });
    await wrapper.get('.memory-file-input').trigger('change');
    await flushPromises();

    expect(wrapper.get('.error-line').text()).toContain('已保存 1 张照片');
    expect(mocks.analyze).toHaveBeenCalledWith('3001');
    expect(mocks.index).toHaveBeenCalledWith('3001');
  });

  it('keeps saved content and never calls the next step when arranging fails', async () => {
    mocks.addPhotos.mockResolvedValue([{ id: '1' }]);
    mocks.analyze.mockRejectedValueOnce(new Error('service unavailable'));
    const wrapper = mountPage();
    await flushPromises();
    const file = new File(['one'], 'one.jpg', { type: 'image/jpeg' });
    Object.defineProperty(wrapper.get('.memory-file-input').element, 'files', { value: [file] });
    await wrapper.get('.memory-file-input').trigger('change');
    await flushPromises();

    expect(mocks.index).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('照片已保存，但暂时没能整理');
    expect(wrapper.text()).toContain('西湖');
    expect(wrapper.get('.memory-update-state button').text()).toBe('重新整理');
  });

  it('keeps the timeline available when detail search preparation fails and retries only that step', async () => {
    mocks.addPhotos.mockResolvedValue([{ id: '1' }]);
    mocks.index.mockRejectedValueOnce(new Error('service unavailable')).mockResolvedValueOnce({ indexedItems: 3 });
    const wrapper = mountPage();
    await flushPromises();
    const file = new File(['one'], 'one.jpg', { type: 'image/jpeg' });
    Object.defineProperty(wrapper.get('.memory-file-input').element, 'files', { value: [file] });
    await wrapper.get('.memory-file-input').trigger('change');
    await flushPromises();

    expect(wrapper.text()).toContain('旅行时间线');
    await wrapper.get('[aria-controls="memory-ask-panel"]').trigger('click');
    expect(wrapper.get('.memory-find-notice').text()).toContain('记录已更新，暂时不能查找旅行细节');
    await wrapper.get('.memory-find-notice button').trigger('click');
    await flushPromises();
    expect(mocks.analyze).toHaveBeenCalledTimes(1);
    expect(mocks.index).toHaveBeenCalledTimes(2);
  });

  it('runs suggested questions and locates Snowflake citations without number coercion', async () => {
    mocks.ask.mockResolvedValue({
      answer: '第二天去了灵隐寺。', fallback: true,
      citations: [{ memoryItemId: '996889308694955191', sourceType: 'trip_item', sourceId: '996889308694955101', excerpt: '西湖记录' }],
    });
    const wrapper = mountPage({ attachTo: document.body });
    await flushPromises();
    await wrapper.get('[aria-controls="memory-ask-panel"]').trigger('click');
    await wrapper.findAll('.memory-suggestions button')[0].trigger('click');
    await flushPromises();
    expect(mocks.ask).toHaveBeenCalledWith('3001', '我们第二天去了哪里？');

    await wrapper.get('.memory-citations button').trigger('click');
    expect(wrapper.get('#memory-item-996889308694955191').classes()).toContain('is-focused');
    expect(wrapper.get('.memory-citations em').text()).toContain('查看这条记录');
    wrapper.unmount();
  });

  it('publishes one confirmed cover and explains deletion keeps the original trip', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const wrapper = mountPage();
    await flushPromises();
    await wrapper.get('[aria-controls="memory-share-panel"]').trigger('click');
    expect(wrapper.get('.memory-share-scope').text()).toContain('会公开');
    expect(wrapper.get('.memory-share-scope').text()).toContain('不会公开');
    const radios = wrapper.findAll('.memory-cover-choice input[type="radio"]');
    await radios[1].setValue(true);
    await wrapper.get('.memory-confirm input').setValue(true);
    await wrapper.get('.memory-share-form > .btn-coral').trigger('click');
    await flushPromises();
    expect(mocks.publish).toHaveBeenCalledWith('3001', {
      title: '杭州两日游', note: '', tags: '', photo_item_id: '996889308694955193',
    });

    await wrapper.get('.memory-delete').trigger('click');
    await flushPromises();
    expect(confirm.mock.calls.at(-1)[0]).toContain('“我的行程”仍会保留');
    expect(mocks.remove).toHaveBeenCalledWith('3001');
  });

  it('updates the remaining timeline after a confirmed photo deletion', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const wrapper = mountPage();
    await flushPromises();
    await wrapper.get('.memory-photo-remove').trigger('click');
    await flushPromises();

    expect(mocks.removeItem).toHaveBeenCalledWith('3001', '996889308694955193');
    expect(mocks.analyze).toHaveBeenCalledWith('3001');
    expect(mocks.index).toHaveBeenCalledWith('3001');
  });
});
