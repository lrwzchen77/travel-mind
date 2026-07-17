import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ExploreMapView from './ExploreMapView.vue';

const mocks = vi.hoisted(() => ({
  publicMap: vi.fn(),
  push: vi.fn(),
  replace: vi.fn(),
  route: { query: { city: '杭州' } },
}));

vi.mock('../api/trip.js', () => ({ tripApi: { publicMap: mocks.publicMap } }));
vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => ({ push: mocks.push, replace: mocks.replace }),
}));
vi.mock('../components/map/AsyncTravelMap3D.vue', () => ({
  default: { template: '<div class="map-stub">3D map</div>' },
}));

const data = {
  city: '杭州',
  weather: {
    temperature: 31.4,
    condition: '多云',
    wind_speed: 8,
    updated_at: '2026-07-17T13:00:00+08:00',
    daily: Array.from({ length: 16 }, (_, index) => ({
      date: `2026-07-${String(index + 17).padStart(2, '0')}`,
      dayWeather: index % 3 ? '多云' : '晴',
      nightWeather: '多云',
      dayTemp: 37 - Math.floor(index / 4),
      nightTemp: 29 - Math.floor(index / 6),
    })),
  },
  places: [
    { id: 'attraction-0', name: '西湖', kind: 'attraction', longitude: 120.1485, latitude: 30.242 },
    { id: 'hotel-1', name: '湖畔酒店', kind: 'hotel', longitude: 120.15, latitude: 30.25 },
  ],
  route: null,
  airport: { code: 'HGH', name: '杭州萧山国际机场', longitude: 120.4344, latitude: 30.2295 },
  railway_check: { url: 'https://www.12306.cn/index/' },
};

describe('3D 旅行情报地图', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.route.query = { city: '杭州' };
    mocks.publicMap.mockResolvedValue(data);
  });

  it('加载结构化公开数据并把用户选中的地点带入规划', async () => {
    const wrapper = mount(ExploreMapView);
    await flushPromises();

    expect(mocks.publicMap).toHaveBeenCalledWith('杭州');
    expect(wrapper.text()).toContain('31°');
    expect(wrapper.text()).toContain('西湖');
    expect(wrapper.text()).toContain('HGH · 杭州萧山国际机场');
    expect(wrapper.text()).not.toContain('点一下定位，也可加入规划');
    expect(wrapper.text()).not.toContain('公开资料快照，不是实时航班接口');

    await wrapper.get('.intel-card--place').trigger('click');
    await wrapper.get('.plan-cta').trigger('click');

    expect(mocks.push).toHaveBeenCalledWith({ path: '/planning', query: { city: '杭州', poi: '西湖' } });
  });

  it('从当前天气摘要打开可纵向浏览的 16 日天气面板', async () => {
    const wrapper = mount(ExploreMapView, { attachTo: document.body });
    await flushPromises();

    expect(wrapper.find('.forecast-list').exists()).toBe(false);
    await wrapper.get('.forecast-entry').trigger('click');

    expect(document.body.querySelector('[role="dialog"]')).not.toBeNull();
    expect(document.body.querySelectorAll('.forecast-day')).toHaveLength(16);
    expect(document.body.textContent).toContain('今天 · 7月17日');
    expect(document.body.querySelector('.temperature-track')).not.toBeNull();
    expect(document.activeElement).toBe(document.body.querySelector('[aria-label="关闭未来天气"]'));

    document.body.querySelector('[aria-label="关闭未来天气"]')
      .dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true }));
    expect(document.activeElement).toBe(document.body.querySelector('.forecast-list'));

    document.body.querySelector('[aria-label="关闭未来天气"]').click();
    await wrapper.vm.$nextTick();
    expect(document.body.querySelector('[role="dialog"]')).toBeNull();
    wrapper.unmount();
  });

  it('公开接口失败时仍给出可继续使用地图的指引', async () => {
    mocks.publicMap.mockRejectedValueOnce(new Error('offline'));
    const wrapper = mount(ExploreMapView);
    await flushPromises();

    expect(wrapper.text()).toContain('附近内容暂时没加载出来');
    expect(wrapper.text()).toContain('地图仍然可以逛');
  });
});
