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
  default: {
    props: ['initialTrackPoints', 'publicData'],
    template: `<button class="map-stub" :data-initial-count="initialTrackPoints.length" :data-popup-facts="publicData?.places?.[0]?.facts" :data-popup-source="publicData?.places?.[0]?.source" @click="$emit('track-plan', {
      city: '杭州', mode: 'soft_order', nodes: [
        { order: 1, type: 'poi', poi_id: 'west-lake', name: '西湖', kind: 'attraction', longitude: 120.1485, latitude: 30.242 },
        { order: 2, type: 'free_point', name: '自定义节点 2', longitude: 120.1152, latitude: 30.2288 }
      ]
    })">3D map</button>`,
  },
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
    {
      id: 'attraction-0', name: '西湖', kind: 'attraction', longitude: 120.1485, latitude: 30.242,
      category: '湖泊景区', distance_km: 1.6, opening_hours: '全天开放', address: '杭州市西湖区', source: '高德地图',
      rating: 4.8, cost: 0, community_mentions: 3, community_tip: '杭州西湖下午这样走', image_url: 'https://example.com/west-lake.jpg',
    },
    { id: 'hotel-1', name: '湖畔酒店', kind: 'hotel', longitude: 120.15, latitude: 30.25 },
  ],
  route: null,
  airport: { code: 'HGH', name: '杭州萧山国际机场', longitude: 120.4344, latitude: 30.2295 },
  railway_check: { url: 'https://www.12306.cn/index/' },
};

describe('3D 旅行情报地图', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.sessionStorage.clear();
    mocks.route.query = { city: '杭州' };
    mocks.route.path = '/map';
    mocks.publicMap.mockResolvedValue(data);
  });

  it('加载结构化公开数据并把用户选中的地点带入规划', async () => {
    const wrapper = mount(ExploreMapView);
    await flushPromises();

    expect(mocks.publicMap).toHaveBeenCalledWith('杭州', 120.1551, 30.2741);
    expect(wrapper.text()).toContain('31°');
    expect(wrapper.text()).toContain('西湖');
    expect(wrapper.get('.intel-card--place img').attributes('src')).toBe('https://example.com/west-lake.jpg');
    expect(wrapper.get('.intel-card--place').text()).not.toContain('评分 4.8');
    expect(wrapper.get('.map-stub').attributes('data-popup-facts')).toBe('评分 4.8 · 距中心 1.6 km · 营业 全天开放 · 杭州市西湖区');
    expect(wrapper.text()).toContain('HGH · 杭州萧山国际机场');
    expect(wrapper.text()).not.toContain('点一下定位，也可加入规划');
    expect(wrapper.text()).not.toContain('公开资料快照，不是实时航班接口');

    await wrapper.findAll('.intel-card--place')[0].trigger('click');
    await wrapper.findAll('.intel-card--place')[1].trigger('click');
    await wrapper.get('.plan-cta').trigger('click');

    expect(JSON.parse(window.sessionStorage.getItem('travelmind.route-intent'))).toMatchObject({
      city: '杭州', nodes: [{ name: '西湖' }, { name: '湖畔酒店' }],
    });
    expect(mocks.push).toHaveBeenCalledWith({ path: '/planning', query: { city: '杭州', route: '1' } });
  });

  it('地图工作台以地图为主且不嵌入路线确认页', async () => {
    const wrapper = mount(ExploreMapView);
    await flushPromises();

    expect(wrapper.find('.intelligence-map').exists()).toBe(true);
    expect(wrapper.find('.planning-stub').exists()).toBe(false);
    expect(wrapper.text()).toContain('地图是行程起点');
    expect(wrapper.text()).toContain('杭州，先圈出想去的地方');
  });

  it('少于两个地点时禁用规划按钮并显示还差几个地点', async () => {
    const wrapper = mount(ExploreMapView);
    await flushPromises();

    expect(wrapper.get('.plan-cta').attributes('disabled')).toBeDefined();
    expect(wrapper.get('.plan-cta').text()).toBe('还差 2 个地点');
    await wrapper.get('.intel-card--place').trigger('click');

    expect(mocks.push).not.toHaveBeenCalled();
    expect(wrapper.get('.plan-cta').attributes('disabled')).toBeDefined();
    expect(wrapper.get('.plan-cta').text()).toBe('还差 1 个地点');
  });

  it('把地图轨迹保存到会话并带入规划页', async () => {
    mocks.route.query = { city: '杭州', note: '少走路', inspirationIds: '12,18' };
    const wrapper = mount(ExploreMapView);
    await flushPromises();

    await wrapper.get('.map-stub').trigger('click');

    expect(JSON.parse(window.sessionStorage.getItem('travelmind.route-intent'))).toMatchObject({
      city: '杭州', mode: 'soft_order', nodes: [{ poi_id: 'west-lake' }, { type: 'free_point' }],
    });
    expect(mocks.push).toHaveBeenCalledWith({
      path: '/planning',
      query: { city: '杭州', note: '少走路', inspirationIds: '12,18', route: '1' },
    });
  });

  it('从确认页返回地图时恢复尚未提交的路线', async () => {
    window.sessionStorage.setItem('travelmind.route-intent', JSON.stringify({
      city: '杭州', mode: 'soft_order', nodes: [
        { order: 1, type: 'poi', poi_id: 'west-lake', name: '西湖', longitude: 120.1485, latitude: 30.242 },
        { order: 2, type: 'free_point', name: '龙井路慢游段', longitude: 120.1152, latitude: 30.2288 },
      ],
    }));

    const wrapper = mount(ExploreMapView);
    await flushPromises();

    expect(wrapper.get('.map-stub').attributes('data-initial-count')).toBe('2');
    expect(wrapper.text()).toContain('路线已连成 2 个节点');
  });

  it('合并精选地点作为空数据兜底，并按规范化名称去重', async () => {
    mocks.publicMap.mockResolvedValueOnce({ ...data, places: [] });
    const fallback = mount(ExploreMapView);
    await flushPromises();

    expect(fallback.findAll('.intel-card--place')).toHaveLength(3);
    expect(fallback.get('.map-stub').attributes('data-popup-source')).toBe('Travel Mind 精选');
    fallback.unmount();

    mocks.publicMap.mockResolvedValueOnce({
      ...data,
      places: [{ ...data.places[0], name: '西 湖' }, data.places[1]],
    });
    const merged = mount(ExploreMapView);
    await flushPromises();
    expect(merged.findAll('.intel-card--place')).toHaveLength(4);
    expect(merged.findAll('.intel-card--place').filter((card) => card.text().replaceAll(' ', '').includes('西湖'))).toHaveLength(1);
  });

  it('可从完整城市列表切换目的地并按对应坐标加载数据', async () => {
    const wrapper = mount(ExploreMapView);
    await flushPromises();

    await wrapper.get('.city-picker select').setValue('厦门');
    await flushPromises();

    expect(mocks.publicMap).toHaveBeenLastCalledWith('厦门', 118.0894, 24.4798);
    expect(mocks.replace).toHaveBeenLastCalledWith({ query: { city: '厦门' } });
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
    expect(wrapper.findAll('.intel-card--place')).toHaveLength(3);
  });
});
