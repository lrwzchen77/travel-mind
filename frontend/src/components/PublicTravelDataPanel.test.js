import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import PublicTravelDataPanel from './PublicTravelDataPanel.vue';

describe('PublicTravelDataPanel', () => {
  it('distinguishes live data from demo prices and links railway checks to the official site', () => {
    const wrapper = mount(PublicTravelDataPanel, { props: { items: [
      { title: '当前天气', detail: '杭州 28°C', source: 'Open-Meteo', data_kind: 'live', bookable: false },
      { title: '铁路票价与余票请核验', detail: '不是当前报价', source: '铁路 12306', data_kind: 'demo_reference', bookable: false, url: 'https://www.12306.cn/index/' },
    ] } });

    expect(wrapper.text()).toContain('实时数据');
    expect(wrapper.text()).toContain('演示参考');
    expect(wrapper.text()).toContain('不可在本平台预订');
    expect(wrapper.get('a').attributes('href')).toBe('https://www.12306.cn/index/');
    expect(wrapper.text()).not.toContain('实时天气暂不可用');
  });

  it('states when live weather is unavailable without hiding open data', () => {
    const wrapper = mount(PublicTravelDataPanel, { props: { items: [
      { title: '机场资料', detail: '不含航班时刻', source: 'OurAirports', data_kind: 'open_data', bookable: false },
    ] } });

    expect(wrapper.text()).toContain('实时天气暂不可用');
    expect(wrapper.text()).toContain('机场资料');
    expect(wrapper.text()).toContain('不可在本平台预订');
  });
});
