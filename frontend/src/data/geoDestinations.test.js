import { describe, expect, it } from 'vitest';
import {
  findDestination,
  geoDestinations,
  provincialCapitalCities,
} from './geoDestinations.js';

const expectedProvincialCapitals = [
  '北京', '天津', '石家庄', '太原', '呼和浩特', '沈阳', '长春', '哈尔滨',
  '上海', '南京', '杭州', '合肥', '福州', '南昌', '济南', '郑州', '武汉',
  '长沙', '广州', '南宁', '海口', '重庆', '成都', '贵阳', '昆明', '拉萨',
  '西安', '兰州', '西宁', '银川', '乌鲁木齐', '台北', '香港', '澳门',
];

describe('geo destinations', () => {
  it('covers every provincial-level capital or administrative seat', () => {
    expect(new Set(provincialCapitalCities)).toEqual(new Set(expectedProvincialCapitals));
    expect(provincialCapitalCities).toHaveLength(34);
  });

  it('keeps every destination uniquely addressable on the map', () => {
    const cityNames = geoDestinations.map((destination) => destination.city);
    expect(new Set(cityNames).size).toBe(cityNames.length);

    geoDestinations.forEach((destination) => {
      expect(destination.province).toBeTruthy();
      expect(destination.lng).toBeGreaterThanOrEqual(73);
      expect(destination.lng).toBeLessThanOrEqual(135);
      expect(destination.lat).toBeGreaterThanOrEqual(18);
      expect(destination.lat).toBeLessThanOrEqual(54);
      expect(findDestination(destination.city)).toBe(destination);
    });
  });
});
