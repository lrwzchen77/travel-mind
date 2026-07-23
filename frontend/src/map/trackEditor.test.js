import { describe, expect, it } from 'vitest';
import {
  appendTrackPoint,
  interpolatePoint,
  MAX_TRACK_NODES,
  normalizeRouteIntent,
  routeIntentFromTrack,
  trackDistanceKm,
} from './trackEditor.js';

describe('地图轨迹编辑器', () => {
  it('按点击顺序编号、计算路线，并限制节点数量', () => {
    let points = appendTrackPoint([], { lng: 120.15512345, lat: 30.27412345 });
    points = appendTrackPoint(points, { lng: 120.16512345, lat: 30.28412345 });

    expect(points).toEqual([
      { number: 1, longitude: 120.155123, latitude: 30.274123 },
      { number: 2, longitude: 120.165123, latitude: 30.284123 },
    ]);
    expect(trackDistanceKm(points)).toBeGreaterThan(1);
    expect(interpolatePoint(points[0], points[1], 0.5)).toEqual([120.160123, 30.279123]);

    const snapped = appendTrackPoint(points, { lng: 120.17, lat: 30.29 }, {
      poiId: 'west-lake', name: '西湖', kind: 'attraction',
    });
    expect(snapped.at(-1)).toMatchObject({ number: 3, poiId: 'west-lake', name: '西湖' });
    expect(appendTrackPoint(snapped, { lng: 120.17, lat: 30.29 }, { poiId: 'west-lake' })).toBe(snapped);

    const full = Array.from({ length: MAX_TRACK_NODES }, (_, index) => ({ number: index + 1 }));
    expect(appendTrackPoint(full, { lng: 120, lat: 30 })).toBe(full);
  });

  it('把吸附地点和自由落点序列化为 AI 可用的路线约束', () => {
    const intent = routeIntentFromTrack('杭州', [
      { number: 1, poiId: 'west-lake', name: '西湖', kind: 'attraction', note: '傍晚看日落', preferences: ['必去', '拍照'], longitude: 120.1485, latitude: 30.242 },
      { number: 2, longitude: 120.1152, latitude: 30.2288 },
    ]);

    expect(intent).toEqual({
      city: '杭州', mode: 'soft_order', nodes: [
        { order: 1, type: 'poi', poi_id: 'west-lake', name: '西湖', kind: 'attraction', note: '傍晚看日落', preferences: ['必去', '拍照'], longitude: 120.1485, latitude: 30.242 },
        { order: 2, type: 'free_point', name: '自定义节点 2', longitude: 120.1152, latitude: 30.2288 },
      ],
    });
    expect(normalizeRouteIntent(intent, '杭州')).toEqual(intent);
    expect(normalizeRouteIntent(intent, '成都')).toBeNull();
  });
});
