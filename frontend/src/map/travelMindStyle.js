/**
 * MapLibre 在线矢量样式（OpenFreeMap）
 * 第一版真实路网/建筑 + 项目主题色
 */

export const MAP_THEME = {
  sky: '#eef4ff',
  sand: '#fff8f3',
  land: '#f3f1ec',
  landSoft: '#ebe8e2',
  water: '#9ec9d4',
  waterDeep: '#7eb3c2',
  park: '#d5e8df',
  wood: '#cfe0d4',
  road: '#e7edf2',
  roadCase: '#c9d6e0',
  roadMajor: '#d5e0e8',
  roadMajorCase: '#b7c8d4',
  building: '#d2dee6',
  buildingTop: '#e8f0f4',
  building3dLow: '#d8e4ec',
  building3dMid: '#b9ccd8',
  building3dHigh: '#8fadbd',
  label: '#2a3f55',
  labelHalo: 'rgba(255, 248, 243, 0.92)',
  muted: '#6b7a8c',
  ocean: '#1f7a8c',
  coral: '#ff6b4a',
  boundary: '#c5d5de',
  trail: '#1f7a8c',
  trailGlow: '#ff6b4a',
};

const nameField = [
  'coalesce',
  ['get', 'name:zh'],
  ['get', 'name_zh'],
  ['get', 'name:zh-Hans'],
  ['get', 'name'],
];

/** 精简主题样式 + OpenFreeMap 全球矢量瓦片（真实路网） */
export function createTravelMindStyle() {
  const t = MAP_THEME;
  return {
    version: 8,
    name: 'Travel Mind Online',
    sources: {
      openmaptiles: {
        type: 'vector',
        url: 'https://tiles.openfreemap.org/planet',
      },
    },
    glyphs: 'https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf',
    layers: [
      {
        id: 'background',
        type: 'background',
        paint: { 'background-color': t.land },
      },
      {
        id: 'landcover',
        type: 'fill',
        source: 'openmaptiles',
        'source-layer': 'landcover',
        maxzoom: 14,
        paint: {
          'fill-color': [
            'match',
            ['get', 'class'],
            'wood', t.wood,
            'grass', t.park,
            'sand', t.sand,
            t.landSoft,
          ],
          'fill-opacity': 0.5,
        },
      },
      {
        id: 'landuse',
        type: 'fill',
        source: 'openmaptiles',
        'source-layer': 'landuse',
        maxzoom: 14,
        filter: ['in', ['get', 'class'], ['literal', ['residential', 'suburb', 'neighbourhood']]],
        paint: { 'fill-color': '#f7fafc', 'fill-opacity': 0.5 },
      },
      {
        id: 'park',
        type: 'fill',
        source: 'openmaptiles',
        'source-layer': 'park',
        paint: { 'fill-color': t.park, 'fill-opacity': 0.65 },
      },
      {
        id: 'water',
        type: 'fill',
        source: 'openmaptiles',
        'source-layer': 'water',
        paint: { 'fill-color': t.water },
      },
      {
        id: 'waterway',
        type: 'line',
        source: 'openmaptiles',
        'source-layer': 'waterway',
        minzoom: 8,
        paint: {
          'line-color': t.waterDeep,
          'line-width': ['interpolate', ['linear'], ['zoom'], 8, 0.5, 14, 2.2],
          'line-opacity': 0.85,
        },
      },
      {
        id: 'boundary',
        type: 'line',
        source: 'openmaptiles',
        'source-layer': 'boundary',
        filter: ['<=', ['get', 'admin_level'], 4],
        paint: {
          'line-color': t.boundary,
          'line-width': 0.8,
          'line-dasharray': [2, 2],
          'line-opacity': 0.65,
        },
      },
      {
        id: 'road-minor-case',
        type: 'line',
        source: 'openmaptiles',
        'source-layer': 'transportation',
        minzoom: 12,
        filter: ['in', ['get', 'class'], ['literal', ['minor', 'service', 'path', 'track']]],
        paint: {
          'line-color': t.roadCase,
          'line-width': ['interpolate', ['linear'], ['zoom'], 12, 1.2, 16, 4],
        },
      },
      {
        id: 'road-minor',
        type: 'line',
        source: 'openmaptiles',
        'source-layer': 'transportation',
        minzoom: 12,
        filter: ['in', ['get', 'class'], ['literal', ['minor', 'service', 'path', 'track']]],
        paint: {
          'line-color': t.road,
          'line-width': ['interpolate', ['linear'], ['zoom'], 12, 0.6, 16, 2.5],
        },
      },
      {
        id: 'road-major-case',
        type: 'line',
        source: 'openmaptiles',
        'source-layer': 'transportation',
        minzoom: 8,
        filter: ['in', ['get', 'class'], ['literal', ['primary', 'secondary', 'tertiary', 'trunk', 'motorway']]],
        layout: { 'line-cap': 'round', 'line-join': 'round' },
        paint: {
          'line-color': t.roadMajorCase,
          'line-width': ['interpolate', ['linear'], ['zoom'], 8, 1.2, 12, 4, 16, 10],
        },
      },
      {
        id: 'road-major',
        type: 'line',
        source: 'openmaptiles',
        'source-layer': 'transportation',
        minzoom: 8,
        filter: ['in', ['get', 'class'], ['literal', ['primary', 'secondary', 'tertiary', 'trunk', 'motorway']]],
        layout: { 'line-cap': 'round', 'line-join': 'round' },
        paint: {
          'line-color': t.roadMajor,
          'line-width': ['interpolate', ['linear'], ['zoom'], 8, 0.6, 12, 2.2, 16, 6],
        },
      },
      {
        id: 'building',
        type: 'fill',
        source: 'openmaptiles',
        'source-layer': 'building',
        minzoom: 13,
        maxzoom: 15,
        paint: {
          'fill-color': t.building,
          'fill-opacity': 0.75,
          'fill-outline-color': t.buildingTop,
        },
      },
      {
        id: 'building-3d',
        type: 'fill-extrusion',
        source: 'openmaptiles',
        'source-layer': 'building',
        minzoom: 14,
        paint: {
          'fill-extrusion-color': [
            'interpolate',
            ['linear'],
            ['coalesce', ['get', 'render_height'], ['get', 'height'], 12],
            0, t.building3dLow,
            30, t.building3dMid,
            90, t.building3dHigh,
          ],
          'fill-extrusion-height': [
            'coalesce',
            ['get', 'render_height'],
            ['get', 'height'],
            14,
          ],
          'fill-extrusion-base': [
            'coalesce',
            ['get', 'render_min_height'],
            ['get', 'min_height'],
            0,
          ],
          'fill-extrusion-opacity': 0.88,
        },
      },
      {
        id: 'place-city',
        type: 'symbol',
        source: 'openmaptiles',
        'source-layer': 'place',
        filter: ['in', ['get', 'class'], ['literal', ['city', 'town']]],
        layout: {
          'text-field': nameField,
          'text-font': ['Noto Sans Bold'],
          'text-size': ['interpolate', ['linear'], ['zoom'], 4, 11, 10, 16],
          'text-padding': 12,
          'text-max-width': 8,
        },
        paint: {
          'text-color': t.label,
          'text-halo-color': t.labelHalo,
          'text-halo-width': 1.4,
        },
      },
      // 飞航图层（空数据，运行时 setData）
      {
        id: 'flight-trail-glow',
        type: 'line',
        source: {
          type: 'geojson',
          data: { type: 'FeatureCollection', features: [] },
          lineMetrics: true,
        },
        layout: { 'line-cap': 'round', 'line-join': 'round' },
        paint: {
          'line-color': t.trailGlow,
          'line-width': 8,
          'line-opacity': 0.18,
          'line-blur': 1.2,
        },
      },
    ],
  };
}

/**
 * 使用完整 Liberty 底图（第一版真实感）+ 运行时叠加飞航层
 * 比自绘层更完整，视觉接近最初版本
 */
export function getMapStyle() {
  if (import.meta.env.VITE_MAP_STYLE_URL) {
    return import.meta.env.VITE_MAP_STYLE_URL;
  }
  // 第一版同款在线样式
  return 'https://tiles.openfreemap.org/styles/liberty';
}

export function emptyFc() {
  return { type: 'FeatureCollection', features: [] };
}

export function lineFc(coords, props = {}) {
  return {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        properties: props,
        geometry: { type: 'LineString', coordinates: coords },
      },
    ],
  };
}
