import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import RecommendationView from './RecommendationView.vue';

const mocks = vi.hoisted(() => ({ list: vi.fn(), feedback: vi.fn() }));

vi.mock('../api/recommendation.js', () => ({ recommendationApi: mocks }));
vi.mock('../composables/useReveal.js', () => ({ useReveal: vi.fn() }));
vi.mock('../components/PagePrologue.vue', () => ({ default: { template: '<header />' } }));

describe('智能推荐页', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.list.mockResolvedValue([{ itemType: 'hotel', itemId: 8, name: '湖畔酒店', city: '杭州', rating: 4.5 }]);
    mocks.feedback.mockResolvedValue({});
  });

  it('submits one feedback choice and then locks both actions', async () => {
    const wrapper = mount(RecommendationView, {
      global: { stubs: { RouterLink: { props: ['to'], template: '<a><slot /></a>' } } },
    });
    await flushPromises();

    await wrapper.get('[aria-label="喜欢这条推荐"]').trigger('click');
    await flushPromises();

    expect(mocks.feedback).toHaveBeenCalledWith(8, 'hotel', 'like');
    expect(wrapper.get('[aria-label="喜欢这条推荐"]').attributes('disabled')).toBeDefined();
    expect(wrapper.get('[aria-label="对这条推荐不感兴趣"]').attributes('disabled')).toBeDefined();
  });
});
