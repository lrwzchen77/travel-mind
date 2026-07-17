import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MemoryDetailView from './MemoryDetailView.vue';

const mocks = vi.hoisted(() => ({
  addPhotos: vi.fn(), analyze: vi.fn(), ask: vi.fn(), detail: vi.fn(), index: vi.fn(),
  publish: vi.fn(), remove: vi.fn(), removeItem: vi.fn(), push: vi.fn(),
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
  useRoute: () => ({ params: { id: '3001' } }),
  useRouter: () => ({ push: mocks.push }),
}));

const memory = {
  id: 3001, title: '杭州两日游', destination_city: '杭州', summary: '慢游西湖', visibility: 'private',
  generation_status: 'ready', index_status: 'ready',
  items: [
    { id: '996889308694955191', item_type: 'place', source_type: 'trip_item', place_name: '西湖', content: '湖边慢游', day_index: 1 },
    { id: '996889308694955192', item_type: 'expense', source_type: 'trip_expense', place_name: '龙井体验', content: 'food，¥188', day_index: 1 },
    { id: '996889308694955193', item_type: 'photo', source_url: '/uploads/photo.jpg', place_name: '断桥', ai_caption: '断桥，湖景，照片', ai_tags: '["湖景"]', day_index: 1 },
  ],
  generations: [{ generation_type: 'timeline', evidence_json: '["996889308694955191","996889308694955193"]', version: 2 }],
};

beforeEach(() => {
  vi.clearAllMocks();
  vi.restoreAllMocks();
  mocks.detail.mockResolvedValue(structuredClone(memory));
  mocks.addPhotos.mockResolvedValue([]);
  mocks.analyze.mockResolvedValue({ version: 3 });
  mocks.index.mockResolvedValue({ indexedItems: 3 });
  mocks.publish.mockResolvedValue({ id: 7001, status: 0 });
  mocks.removeItem.mockResolvedValue();
});

describe('旅行记忆详情', () => {
  it('shows a private evidence timeline without exposing coordinates', async () => {
    const wrapper = mount(MemoryDetailView);
    await flushPromises();

    expect(wrapper.text()).toContain('仅自己可见');
    expect(wrapper.text()).toContain('DAY 01');
    expect(wrapper.text()).toContain('消费明细仅在私有记忆册显示');
    expect(wrapper.findAll('.memory-item.is-evidence')).toHaveLength(2);
    expect(wrapper.text()).not.toContain('经度');
    expect(wrapper.text()).not.toContain('纬度');
  });

  it('uploads several real files then can analyze and rebuild the index', async () => {
    const wrapper = mount(MemoryDetailView);
    await flushPromises();
    const files = [
      new File(['one'], 'one.jpg', { type: 'image/jpeg' }),
      new File(['two'], 'two.png', { type: 'image/png' }),
    ];
    Object.defineProperty(wrapper.get('.memory-file-input').element, 'files', { value: files });
    await wrapper.get('.memory-file-input').trigger('change');
    await flushPromises();
    expect(mocks.addPhotos).toHaveBeenCalledWith('3001', files);

    const actions = wrapper.findAll('.memory-control-strip .text-link');
    await actions[1].trigger('click');
    await flushPromises();
    await actions[2].trigger('click');
    await flushPromises();
    expect(mocks.analyze).toHaveBeenCalledWith('3001');
    expect(mocks.index).toHaveBeenCalledWith('3001');
  });

  it('locates cited evidence and publishes only a confirmed 0-or-1 cover intent', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    mocks.ask.mockResolvedValue({
      answer: '根据这次旅行记录：第 1 天，西湖。', fallback: true,
      citations: [{ memoryItemId: '996889308694955191', sourceType: 'trip_item', sourceId: '996889308694955101', excerpt: '第 1 天，西湖' }],
    });
    const wrapper = mount(MemoryDetailView, { attachTo: document.body });
    await flushPromises();
    await wrapper.get('.memory-ask form').trigger('submit');
    await flushPromises();
    await wrapper.get('.memory-citations button').trigger('click');
    expect(wrapper.get('#memory-item-996889308694955191').classes()).toContain('is-focused');

    await wrapper.get('.memory-hero-actions .btn-ghost').trigger('click');
    const radios = wrapper.findAll('.memory-cover-choice input[type="radio"]');
    await radios[1].setValue(true);
    await wrapper.get('.memory-confirm input').setValue(true);
    await wrapper.get('.memory-share-form > .btn-coral').trigger('click');
    await flushPromises();
    expect(mocks.publish).toHaveBeenCalledWith('3001', {
      title: '杭州两日游', note: '', tags: '', photo_item_id: '996889308694955193',
    });
    expect(wrapper.text()).toContain('脱敏副本已提交审核');
    wrapper.unmount();
  });
});
