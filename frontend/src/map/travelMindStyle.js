/**
 * MapLibre 在线矢量样式（OpenFreeMap）
 * 第一版真实路网/建筑 + 项目主题色
 */

export const MAP_THEME = {
  sky: '#fff8ef',
  sand: '#fff1df',
  land: '#f3ede4',
  landSoft: '#e9e0d5',
  water: '#b9cbd4',
  waterDeep: '#94b0bc',
  park: '#dce8d6',
  wood: '#cedec8',
  road: '#fffdf8',
  roadCase: '#ded5ca',
  roadMajor: '#fff8ef',
  roadMajorCase: '#d4c4b3',
  building: '#ddd4c9',
  buildingTop: '#f5eee5',
  building3dLow: '#e6ddd3',
  building3dMid: '#cfc0b1',
  building3dHigh: '#ae9b88',
  label: '#4f473f',
  labelHalo: 'rgba(255, 253, 248, 0.94)',
  muted: '#746b61',
  ocean: '#ff7a3d',
  coral: '#ff7a3d',
  boundary: '#d4c4b3',
  trail: '#ff7a3d',
  trailGlow: '#ff7a3d',
};

const nameField = [
  'coalesce',
  ['get', 'name:zh'],
  ['get', 'name_zh'],
  ['get', 'name:zh-Hans'],
  ['get', 'name'],
];

const DEFAULT_MAP_ASSET_BASE_URL = 'https://tiles.openfreemap.org';

export function getMapAssetBaseUrl() {
  return (import.meta.env.VITE_MAP_ASSET_BASE_URL || DEFAULT_MAP_ASSET_BASE_URL).replace(/\/+$/, '');
}

/** 精简主题样式 + OpenFreeMap 全球矢量瓦片（真实路网） */
export function createTravelMindStyle() {
  const t = MAP_THEME;
  const assetBaseUrl = getMapAssetBaseUrl();
  return {
    version: 8,
    name: 'Travel Mind Online',
    sources: {
      openmaptiles: {
        type: 'vector',
        url: `${assetBaseUrl}/planet`,
        attribution: '<a href="https://openfreemap.org/">OpenFreeMap</a> © <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
      },
    },
    glyphs: `${assetBaseUrl}/fonts/{fontstack}/{range}.pbf`,
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
        paint: { 'fill-color': '#fff8ef', 'fill-opacity': 0.5 },
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
    ],
  };
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
