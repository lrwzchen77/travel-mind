import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import TripHistoryView from './TripHistoryView.vue';

const history = vi.hoisted(() => vi.fn());
vi.mock('../api/trip.js', () => ({ tripApi: { history } }));
vi.mock('vue-router', () => ({ RouterLink: { props: ['to'], template: '<a><slot /></a>' } }));

describe('行程册', () => {
  beforeEach(() => history.mockResolvedValue({ items: [{ id: 1, destination_city: '杭州', status: 'saved' }], total: 22 }));

  it('兼容小写状态并通过增大 limit 加载剩余行程', async () => {
    const wrapper = mount(TripHistoryView);
    await flushPromises();
    expect(wrapper.text()).toContain('已规划');
    expect(history).toHaveBeenCalledWith(20);

    history.mockResolvedValueOnce({ items: [{ id: 1, status: 'saved' }, { id: 2, status: 'COMPLETED' }], total: 2 });
    await wrapper.get('.load-more button').trigger('click');
    await flushPromises();
    expect(history).toHaveBeenLastCalledWith(40);
    expect(wrapper.text()).toContain('已结束');
  });
});
