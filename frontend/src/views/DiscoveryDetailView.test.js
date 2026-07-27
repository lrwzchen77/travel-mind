import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import DiscoveryDetailView from './DiscoveryDetailView.vue';

const discoverDetail = vi.hoisted(() => vi.fn((resourceKey) => Promise.resolve(resourceKey === 'cities'
  ? { id: 2, name: '北京' }
  : { id: 7, city_id: 2, name: '故宫', category: '历史文化', rating: 4.9, cost: 60, address: '东城区', description: '沿中轴线参观。', tags: '古建 历史' })));
vi.mock('../api/resources.js', () => ({ resourceApi: { discoverDetail } }));
vi.mock('vue-router', () => ({
  RouterLink: { props: ['to'], template: '<a :data-to="JSON.stringify(to)"><slot /></a>' },
  useRoute: () => ({ params: { resourceKey: 'attractions', id: '7' } }),
}));

describe('公共资源详情', () => {
  it('loads one public resource and offers a planning action', async () => {
    const wrapper = mount(DiscoveryDetailView);
    await flushPromises();

    expect(discoverDetail).toHaveBeenCalledWith('attractions', '7');
    expect(discoverDetail).toHaveBeenCalledWith('cities', 2);
    expect(wrapper.text()).toContain('故宫');
    expect(wrapper.text()).toContain('沿中轴线参观');
    const to = JSON.parse(wrapper.get('.btn-coral').attributes('data-to'));
    expect(to).toMatchObject({ path: '/map', query: { city: '北京', resourceType: 'attractions', resourceName: '故宫' } });
  });
});
