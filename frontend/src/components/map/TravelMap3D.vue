<script setup>
/**
 * MapLibre + 可配置瓦片镜像
 * 首帧优先展示，标注与 3D 渐进增强，并按设备能力控制渲染开销。
 */
import { computed, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue';
import { X, ArrowLeft, ArrowRight, Undo2, Plus, Pause, ZoomIn, ZoomOut, Minus } from 'lucide-vue-next';
import { findDestination, geoDestinations } from '../../data/geoDestinations.js';
import { buildFlightArc, sampleArc } from '../../map/flightPath.js';
import { detectMapPerformanceProfile } from '../../map/performance.js';
import { prefetchFlightTiles } from '../../map/tilePrefetch.js';
import { emptyFc, lineFc, MAP_THEME } from '../../map/travelMindStyle.js';
import {
  appendTrackPoint,
  interpolatePoint,
  MAX_TRACK_NODES,
  normalizeTrackPoints,
  routeIntentFromTrack,
  trackDistanceKm,
} from '../../map/trackEditor.js';
import {
  prefetchMapLibre,
  preconnectMapCdn,
  resolveMapStyle,
} from '../../map/prefetch.js';

const props = defineProps({
  city: { type: String, default: '杭州' },
  height: { type: String, default: '480px' },
  showPois: { type: Boolean, default: true },
  autoOrbit: { type: Boolean, default: false },
  compact: { type: Boolean, default: false },
  interactive: { type: Boolean, default: true },
  publicData: { type: Object, default: null },
  selectedPlaceIds: { type: Array, default: () => [] },
  selectableCities: { type: Array, default: () => [] },
  trackEditor: { type: Boolean, default: false },
  initialTrackPoints: { type: Array, default: () => [] },
});

const emit = defineEmits(['city-change', 'point-select', 'track-change', 'track-plan', 'ready', 'error']);

const containerRef = ref(null);
const mapRef = shallowRef(null);
const status = ref('loading'); // loading | ready | error
const loadPhase = ref('连接地图服务…');
const errorMsg = ref('');
const activeCity = ref(props.city);
const orbiting = ref(false);
const flying = ref(false);
const flyLabel = ref('');
const flyProgress = ref(0);
const trackEditing = ref(false);
const trackPoints = ref(normalizeTrackPoints(props.initialTrackPoints));
const selectedTrackIndex = ref(-1);
const trackDistance = computed(() => trackDistanceKm(trackPoints.value));
const lastTrackPoint = computed(() => trackPoints.value.at(-1) || null);
const selectedTrackPoint = computed(() => trackPoints.value[selectedTrackIndex.value] || null);
const NODE_PREFERENCES = ['必去', '慢游', '拍照', '美食', '亲子', '避开人群'];
/** 样式首帧可用后即展示，瓦片和增强图层继续渐进加载。 */
const mapRevealed = ref(false);
const performanceProfile = detectMapPerformanceProfile();
const mapDestinations = computed(() => props.selectableCities.length
  ? geoDestinations.filter((item) => props.selectableCities.includes(item.city))
  : geoDestinations);

/** @type {Map<string, import('maplibre-gl').Marker>} */
const cityMarkers = new Map();
/** @type {import('maplibre-gl').Marker[]} */
const poiMarkers = [];
/** @type {import('maplibre-gl').Marker[]} */
const publicMarkers = [];
/** @type {import('maplibre-gl').Marker[]} */
const trackMarkers = [];

// POI 图标采用 Lucide 的 stroke 风格 path 数据，保持与全局 Lucide 图标视觉一致。
// 图标对应：Mountain（景点）、Hotel（住宿）、Utensils（餐饮）、Plane（机场）。
const POI_ICON_ATTRS = 'fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"';
const PUBLIC_MARKER_ICONS = {
  attraction: `<svg viewBox="0 0 24 24" aria-hidden="true" ${POI_ICON_ATTRS}><path d="m8 3 4 8 5-5 5 15H2L8 3z"/></svg>`,
  hotel: `<svg viewBox="0 0 24 24" aria-hidden="true" ${POI_ICON_ATTRS}><path d="M2 20h20M3 20V8l9-5 9 5v12M9 12h.01M15 12h.01M9 16h6"/></svg>`,
  restaurant: `<svg viewBox="0 0 24 24" aria-hidden="true" ${POI_ICON_ATTRS}><path d="M3 2v7c0 1.1.9 2 2 2h2a2 2 0 0 0 2-2V2M7 2v20M21 15V2a5 5 0 0 0-3 9v4h3"/></svg>`,
  airport: `<svg viewBox="0 0 24 24" aria-hidden="true" ${POI_ICON_ATTRS}><path d="M17.8 19.2 16 11l3.5-3.5C21 6 21.5 4 19.5 4c-1 0-1.7.3-2.3 1L14 8 6 4 4 6l6 5-3 3-3-.5L3 16l3.5 1L8 21l1.5-1.5L9 16l3-3 6 5 2-2-2-5.8z"/></svg>`,
};

let maplibregl = null;
let orbitRaf = 0;
let flyRaf = 0;
let flyToken = 0;
let resizeObserver;
let resizeRaf = 0;
let revealTimer = 0;
let enrichTimer = 0;
let enrichIdle = 0;
let destroyed = false;
let mapStyleReady = false;
let pendingFlight = null;
let trackRaf = 0;
let publicPopup = null;

function setPrimaryPin(city) {
  cityMarkers.forEach((marker, name) => {
    marker.getElement().classList.toggle('map-pin--primary', name === city);
  });
}

function ensureCityMarkers(map) {
  if (cityMarkers.size || !maplibregl) return;
  mapDestinations.value.forEach((dest) => {
    const el = document.createElement('button');
    el.type = 'button';
    el.className = 'map-pin';
    const dot = document.createElement('span');
    dot.className = 'map-pin-dot';
    const label = document.createElement('span');
    label.className = 'map-pin-label';
    label.textContent = dest.city;
    el.append(dot, label);
    el.title = dest.city;
    el.addEventListener('click', (e) => {
      e.stopPropagation();
      flyToCity(dest.city);
      emit('city-change', dest.city);
    });
    const marker = new maplibregl.Marker({ element: el, anchor: 'bottom' })
      .setLngLat([dest.lng, dest.lat])
      .addTo(map);
    cityMarkers.set(dest.city, marker);
  });
  setPrimaryPin(activeCity.value);
}

function publicPlacesFc() {
  const selected = new Set(props.selectedPlaceIds);
  return {
    type: 'FeatureCollection',
    features: (props.publicData?.places || []).map((place) => ({
      type: 'Feature',
      properties: { id: place.id, name: place.name, kind: place.kind, selected: selected.has(place.id) },
      geometry: { type: 'Point', coordinates: [place.longitude, place.latitude] },
    })),
  };
}

function publicRouteFc() {
  const coordinates = (props.publicData?.route?.geometry || [])
    .map((point) => [point.longitude, point.latitude])
    .filter(([lng, lat]) => Number.isFinite(lng) && Number.isFinite(lat));
  return coordinates.length >= 2 ? lineFc(coordinates) : emptyFc();
}

function publicAirportFc() {
  const airport = props.publicData?.airport;
  if (!airport || !Number.isFinite(airport.longitude) || !Number.isFinite(airport.latitude)) return emptyFc();
  return {
    type: 'FeatureCollection',
    features: [{
      type: 'Feature',
      properties: { code: airport.code, name: airport.name },
      geometry: { type: 'Point', coordinates: [airport.longitude, airport.latitude] },
    }],
  };
}

function updatePublicSources(map = mapRef.value) {
  if (!map || !mapStyleReady) return;
  setSrc(map, 'public-pois', publicPlacesFc());
  setSrc(map, 'public-route', publicRouteFc());
  setSrc(map, 'public-airports', publicAirportFc());
  syncPublicMarkers(map);
}

function clearPublicMarkers() {
  publicPopup?.remove();
  publicPopup = null;
  while (publicMarkers.length) publicMarkers.pop().remove();
}

function showPublicPopup(map, item) {
  publicPopup?.remove();
  const projected = map.project([item.longitude, item.latitude]);
  const container = map.getContainer();
  const anchor = `${projected.y < container.clientHeight / 2 ? 'top' : 'bottom'}-${projected.x < container.clientWidth / 2 ? 'left' : 'right'}`;
  const content = document.createElement('article');
  content.className = 'travel-node-detail';
  const visual = document.createElement('div');
  visual.className = 'travel-node-visual';
  const icon = document.createElement('span');
  icon.innerHTML = PUBLIC_MARKER_ICONS[item.kind] || PUBLIC_MARKER_ICONS.attraction;
  visual.append(icon);
  if (item.image_url) {
    const image = document.createElement('img');
    image.src = item.image_url;
    image.alt = '';
    image.referrerPolicy = 'no-referrer';
    image.addEventListener('error', () => image.remove(), { once: true });
    visual.prepend(image);
  }
  const body = document.createElement('div');
  body.className = 'travel-node-body';
  const meta = document.createElement('span');
  meta.textContent = [item.category || item.kind, item.source].filter(Boolean).join(' · ');
  const heading = document.createElement('div');
  heading.className = 'travel-node-heading';
  const name = document.createElement('strong');
  name.textContent = item.name;
  heading.append(name);
  const rating = Number(item.rating);
  if (Number.isFinite(rating) && rating > 0) {
    const score = document.createElement('b');
    score.textContent = `评分 ${rating.toFixed(1)}`;
    heading.append(score);
  }
  body.append(meta, heading);
  const chips = document.createElement('div');
  chips.className = 'travel-node-facts';
  const facts = [
    Number.isFinite(item.distance_km) ? `距中心 ${item.distance_km.toFixed(1)} km` : '',
    Number.isFinite(item.cost) && item.cost > 0 ? `人均 ¥${Math.round(item.cost)}` : '',
    item.opening_hours ? `营业 ${item.opening_hours}` : '',
  ].filter(Boolean);
  facts.forEach((text) => {
    const chip = document.createElement('span');
    chip.textContent = text;
    chips.append(chip);
  });
  if (chips.childElementCount) body.append(chips);
  if (item.address) {
    const facts = document.createElement('p');
    facts.textContent = item.address;
    body.append(facts);
  } else if (item.facts) {
    const facts = document.createElement('p');
    facts.textContent = item.facts;
    body.append(facts);
  }
  if (item.community_mentions) {
    const community = document.createElement('small');
    community.textContent = `旅友提到 ${item.community_mentions} 次${item.community_tip ? ` · ${item.community_tip}` : ''}`;
    body.append(community);
  }
  content.append(visual, body);
  publicPopup = new maplibregl.Popup({
    anchor,
    closeButton: false,
    closeOnClick: false,
    offset: 22,
    maxWidth: '340px',
    className: 'travel-node-popup',
  })
    .setLngLat([item.longitude, item.latitude])
    .setDOMContent(content)
    .addTo(map);
}

function hidePublicPopup() {
  publicPopup?.remove();
  publicPopup = null;
}

function addPublicMarker(map, item, kind, onClick) {
  if (!maplibregl || !Number.isFinite(item?.longitude) || !Number.isFinite(item?.latitude)) return;
  const el = document.createElement('button');
  el.type = 'button';
  el.className = `public-map-marker public-map-marker--${kind}`;
  if (item.id && props.selectedPlaceIds.includes(item.id)) el.classList.add('is-selected');
  if (item.id && trackPoints.value.some((point) => String(point.poiId) === String(item.id))) el.classList.add('is-in-track');
  if (item.id) el.dataset.poiId = String(item.id);
  el.title = item.name;
  el.setAttribute('aria-label', `${item.name}，在地图上查看`);
  const icon = document.createElement('span');
  icon.innerHTML = PUBLIC_MARKER_ICONS[kind] || PUBLIC_MARKER_ICONS.attraction;
  el.append(icon);
  el.addEventListener('mouseenter', () => showPublicPopup(map, item));
  el.addEventListener('mouseleave', hidePublicPopup);
  el.addEventListener('focus', () => showPublicPopup(map, item));
  el.addEventListener('blur', hidePublicPopup);
  el.addEventListener('click', (event) => {
    event.stopPropagation();
    showPublicPopup(map, item);
    onClick();
  });
  publicMarkers.push(new maplibregl.Marker({ element: el, anchor: 'bottom' })
    .setLngLat([item.longitude, item.latitude])
    .addTo(map));
}

function syncPublicMarkers(map) {
  clearPublicMarkers();
  (props.publicData?.places || []).forEach((place) => {
    addPublicMarker(map, place, place.kind, () => selectPublicPlace(place));
  });
  const airport = props.publicData?.airport;
  if (airport) {
    const point = { ...airport, id: `airport-${airport.code}`, kind: 'airport' };
    addPublicMarker(map, point, 'airport', () => selectAirport(point));
  }
}

function ensurePublicSources(map) {
  if (map.getSource('public-pois')) {
    updatePublicSources(map);
    return;
  }
  map.addSource('public-pois', { type: 'geojson', data: publicPlacesFc() });
  map.addSource('public-route', { type: 'geojson', data: publicRouteFc() });
  map.addSource('public-airports', { type: 'geojson', data: publicAirportFc() });

  map.addLayer({
    id: 'public-route-outline', type: 'line', source: 'public-route',
    layout: { 'line-cap': 'round', 'line-join': 'round' },
    paint: { 'line-color': '#fffaf1', 'line-width': 8, 'line-opacity': 0.92 },
  });
  map.addLayer({
    id: 'public-route-line', type: 'line', source: 'public-route',
    layout: { 'line-cap': 'round', 'line-join': 'round' },
    paint: { 'line-color': MAP_THEME.coral, 'line-width': 4, 'line-opacity': 0.96 },
  });
  map.addLayer({
    id: 'public-poi-labels', type: 'symbol', source: 'public-pois', minzoom: 11,
    layout: {
      'text-field': ['get', 'name'], 'text-font': ['Noto Sans Bold'], 'text-size': 12,
      'text-offset': [0, 2.6], 'text-anchor': 'top', 'text-allow-overlap': false,
    },
    paint: { 'text-color': '#23333b', 'text-halo-color': '#fffaf1', 'text-halo-width': 1.5 },
  });
  map.addLayer({
    id: 'public-airport-label', type: 'symbol', source: 'public-airports',
    layout: {
      'text-field': ['get', 'code'], 'text-font': ['Noto Sans Bold'],
      'text-size': 12, 'text-offset': [0, 2.7], 'text-anchor': 'top',
    },
    paint: { 'text-color': '#173f50', 'text-halo-color': '#fffaf1', 'text-halo-width': 1.5 },
  });
  syncPublicMarkers(map);
}

function clearTrackMarkers() {
  while (trackMarkers.length) trackMarkers.pop().remove();
}

function syncTrackMarkers(map) {
  clearTrackMarkers();
  if (!maplibregl) return;
  trackPoints.value.forEach((point, index) => {
    const el = document.createElement('button');
    el.type = 'button';
    const projected = map.project([point.longitude, point.latitude]);
    const labelLeft = projected.x > map.getContainer().clientWidth * 0.7;
    el.className = `track-node${point.poiId ? ' is-poi' : ''}${labelLeft ? ' label-left' : ''}${index === trackPoints.value.length - 1 ? ' is-latest' : ''}${index === selectedTrackIndex.value ? ' is-selected' : ''}`;
    el.setAttribute('aria-label', `路线节点 ${point.number}${point.name ? `，${point.name}` : ''}`);
    const badge = document.createElement('span');
    badge.textContent = point.number;
    el.append(badge);
    if (point.name) {
      const label = document.createElement('em');
      label.textContent = point.name;
      el.append(label);
    }
    el.addEventListener('click', (event) => {
      event.stopPropagation();
      selectedTrackIndex.value = index;
      syncTrackMarkers(map);
    });
    trackMarkers.push(new maplibregl.Marker({ element: el, anchor: 'center' })
      .setLngLat([point.longitude, point.latitude])
      .addTo(map));
  });
}

function syncPublicMarkerStates() {
  const ids = new Set(trackPoints.value.map((point) => point.poiId).filter(Boolean).map(String));
  const selected = new Set(props.selectedPlaceIds.map(String));
  publicMarkers.forEach((marker) => {
    const el = marker.getElement();
    el.classList.toggle('is-in-track', ids.has(el.dataset.poiId));
    el.classList.toggle('is-selected', selected.has(el.dataset.poiId));
  });
}

function trackLine(points = trackPoints.value) {
  if (points.length < 2) return emptyFc();
  return lineFc(points.map((point) => [point.longitude, point.latitude]));
}

function renderTrack(map = mapRef.value, animate = false) {
  if (!map || !mapStyleReady || !map.getSource('editor-track')) return;
  if (trackRaf) cancelAnimationFrame(trackRaf);
  syncTrackMarkers(map);
  syncPublicMarkerStates();
  if (!animate || performanceProfile.reducedMotion || trackPoints.value.length < 2) {
    setSrc(map, 'editor-track', trackLine());
    return;
  }

  const points = trackPoints.value;
  const from = points.at(-2);
  const to = points.at(-1);
  const settled = points.slice(0, -1).map((point) => [point.longitude, point.latitude]);
  const startedAt = performance.now();
  const grow = (now) => {
    const progress = Math.min(1, (now - startedAt) / 320);
    setSrc(map, 'editor-track', lineFc([...settled, interpolatePoint(from, to, 1 - (1 - progress) ** 3)]));
    if (progress < 1) trackRaf = requestAnimationFrame(grow);
    else trackRaf = 0;
  };
  trackRaf = requestAnimationFrame(grow);
}

function ensureTrackSources(map) {
  if (!map || !mapStyleReady || (!props.trackEditor && !trackPoints.value.length) || map.getSource('editor-track')) return;
  map.addSource('editor-track', { type: 'geojson', data: emptyFc() });
  map.addLayer({
    id: 'editor-track-glow', type: 'line', source: 'editor-track',
    layout: { 'line-cap': 'round', 'line-join': 'round' },
    paint: { 'line-color': '#ff7a3d', 'line-width': 15, 'line-opacity': 0.2, 'line-blur': 2 },
  });
  map.addLayer({
    id: 'editor-track-outline', type: 'line', source: 'editor-track',
    layout: { 'line-cap': 'round', 'line-join': 'round' },
    paint: { 'line-color': '#fffaf1', 'line-width': 8, 'line-opacity': 0.96 },
  });
  map.addLayer({
    id: 'editor-track-line', type: 'line', source: 'editor-track',
    layout: { 'line-cap': 'round', 'line-join': 'round' },
    paint: { 'line-color': '#ff7a3d', 'line-width': 4.5, 'line-opacity': 1 },
  });
  renderTrack(map);
}

function syncTrackCursor() {
  const canvas = mapRef.value?.getCanvas();
  if (canvas) canvas.style.cursor = trackEditing.value ? 'crosshair' : '';
}

function toggleTrackEditing() {
  trackEditing.value = !trackEditing.value;
  if (trackEditing.value) stopOrbit();
  syncTrackCursor();
}

function trackIntent() {
  return routeIntentFromTrack(activeCity.value, trackPoints.value);
}

function commitTrack(next, animate = true) {
  if (next === trackPoints.value) return false;
  trackPoints.value = next;
  if (selectedTrackIndex.value >= next.length) selectedTrackIndex.value = next.length - 1;
  if (next.length >= MAX_TRACK_NODES) trackEditing.value = false;
  syncTrackCursor();
  renderTrack(mapRef.value, animate);
  emit('track-change', trackIntent());
  return true;
}

function addTrackPoint(event) {
  if (!props.trackEditor || !trackEditing.value || flying.value) return;
  if (commitTrack(appendTrackPoint(trackPoints.value, event.lngLat))) {
    selectedTrackIndex.value = trackPoints.value.length - 1;
    syncTrackMarkers(mapRef.value);
  }
}

function addTrackLocation(item) {
  if (!Number.isFinite(item?.longitude) || !Number.isFinite(item?.latitude)) return;
  if (commitTrack(appendTrackPoint(trackPoints.value, {
    lng: item.longitude,
    lat: item.latitude,
  }, {
    poiId: item.id,
    name: item.name || item.code,
    kind: item.kind,
  }))) {
    selectedTrackIndex.value = trackPoints.value.length - 1;
    syncTrackMarkers(mapRef.value);
  }
}

function selectPublicPlace(place) {
  if (props.trackEditor && trackEditing.value) addTrackLocation(place);
  else emit('point-select', place);
}

function selectAirport(airport) {
  if (props.trackEditor && trackEditing.value) addTrackLocation(airport);
  else flyToPoint(airport);
}

function undoTrackPoint() {
  if (!trackPoints.value.length) return;
  commitTrack(trackPoints.value.slice(0, -1), false);
}

function clearTrack() {
  selectedTrackIndex.value = -1;
  commitTrack([], false);
}

function updateSelectedTrackPoint(patch) {
  if (!selectedTrackPoint.value) return;
  const next = trackPoints.value.map((point, index) => index === selectedTrackIndex.value ? { ...point, ...patch } : point);
  trackPoints.value = normalizeTrackPoints(next);
  emit('track-change', trackIntent());
}

function toggleNodePreference(preference) {
  const current = selectedTrackPoint.value?.preferences || [];
  updateSelectedTrackPoint({
    preferences: current.includes(preference)
      ? current.filter((item) => item !== preference)
      : [...current, preference].slice(0, 6),
  });
}

function moveSelectedTrackPoint(offset) {
  const from = selectedTrackIndex.value;
  const to = from + offset;
  if (from < 0 || to < 0 || to >= trackPoints.value.length) return;
  const next = [...trackPoints.value];
  [next[from], next[to]] = [next[to], next[from]];
  selectedTrackIndex.value = to;
  commitTrack(normalizeTrackPoints(next), false);
}

function removeSelectedTrackPoint() {
  if (!selectedTrackPoint.value) return;
  const next = trackPoints.value.filter((_, index) => index !== selectedTrackIndex.value);
  selectedTrackIndex.value = Math.min(selectedTrackIndex.value, next.length - 1);
  commitTrack(normalizeTrackPoints(next), false);
}

function planTrack() {
  if (trackPoints.value.length >= 2) emit('track-plan', trackIntent());
}

function flyToPoint(point) {
  if (!mapRef.value || !Number.isFinite(point?.longitude) || !Number.isFinite(point?.latitude)) return;
  stopOrbit();
  mapRef.value.easeTo({
    center: [point.longitude, point.latitude], zoom: Math.max(mapRef.value.getZoom(), 14.2),
    pitch: Math.min(58, performanceProfile.maxPitch), duration: performanceProfile.reducedMotion ? 0 : 700,
  });
}

function clearPois() {
  while (poiMarkers.length) poiMarkers.pop().remove();
}

function addPoiMarkers(map, dest) {
  clearPois();
  if (!props.showPois || !dest.pois?.length || !maplibregl) return;
  dest.pois.slice(0, performanceProfile.maxPoiMarkers).forEach((poi) => {
    const el = document.createElement('div');
    el.className = 'map-poi';
    const dot = document.createElement('span');
    const name = document.createElement('em');
    name.textContent = poi.name;
    el.append(dot, name);
    poiMarkers.push(
      new maplibregl.Marker({ element: el, anchor: 'left' })
        .setLngLat([poi.lng, poi.lat])
        .addTo(map),
    );
  });
}

function ensureFlightSources(map) {
  if (map.getSource('flight-path')) return;
  map.addSource('flight-path', { type: 'geojson', data: emptyFc() });
  map.addSource('flight-progress', { type: 'geojson', data: emptyFc() });
  map.addSource('flight-plane', { type: 'geojson', data: emptyFc() });

  const add = (layer) => {
    if (!map.getLayer(layer.id)) map.addLayer(layer);
  };
  add({
    id: 'flight-trail-glow',
    type: 'line',
    source: 'flight-path',
    layout: { 'line-cap': 'round', 'line-join': 'round' },
    paint: {
      'line-color': MAP_THEME.coral,
      'line-width': 10,
      'line-opacity': 0.2,
      'line-blur': 1.5,
    },
  });
  add({
    id: 'flight-trail',
    type: 'line',
    source: 'flight-path',
    layout: { 'line-cap': 'round', 'line-join': 'round' },
    paint: {
      'line-color': MAP_THEME.ocean,
      'line-width': 2.5,
      'line-opacity': 0.45,
      'line-dasharray': [1.8, 1.4],
    },
  });
  add({
    id: 'flight-progress-glow',
    type: 'line',
    source: 'flight-progress',
    layout: { 'line-cap': 'round', 'line-join': 'round' },
    paint: {
      'line-color': MAP_THEME.coral,
      'line-width': 10,
      'line-opacity': 0.28,
      'line-blur': 1.2,
    },
  });
  add({
    id: 'flight-progress',
    type: 'line',
    source: 'flight-progress',
    layout: { 'line-cap': 'round', 'line-join': 'round' },
    paint: {
      'line-color': MAP_THEME.coral,
      'line-width': 3.5,
      'line-opacity': 0.95,
    },
  });
  add({
    id: 'flight-plane-glow',
    type: 'circle',
    source: 'flight-plane',
    paint: {
      'circle-radius': 14,
      'circle-color': MAP_THEME.coral,
      'circle-opacity': 0.25,
      'circle-blur': 0.7,
    },
  });
  add({
    id: 'flight-plane',
    type: 'circle',
    source: 'flight-plane',
    paint: {
      'circle-radius': 6,
      'circle-color': '#ffffff',
      'circle-stroke-width': 3,
      'circle-stroke-color': MAP_THEME.coral,
    },
  });
}

function setSrc(map, id, data) {
  const s = map.getSource(id);
  if (s) s.setData(data);
}

function tryEnable3dBuildings(map) {
  if (!performanceProfile.enable3d || map.getLayer('tm-3d-buildings')) return;
  const style = map.getStyle();
  if (!style?.sources || style.layers?.some((layer) => layer.type === 'fill-extrusion')) return;
  const sourceId =
    Object.keys(style.sources).find((id) => style.sources[id].type === 'vector' && id.includes('openmap'))
    || Object.keys(style.sources).find((id) => style.sources[id].type === 'vector');
  if (!sourceId) return;
  try {
    map.addLayer({
      id: 'tm-3d-buildings',
      source: sourceId,
      'source-layer': 'building',
      type: 'fill-extrusion',
      minzoom: 14,
      paint: {
        'fill-extrusion-color': [
          'interpolate',
          ['linear'],
          ['coalesce', ['get', 'render_height'], ['get', 'height'], 12],
          0, MAP_THEME.building3dLow,
          40, MAP_THEME.building3dMid,
          100, MAP_THEME.building3dHigh,
        ],
        'fill-extrusion-height': [
          'coalesce',
          ['get', 'render_height'],
          ['get', 'height'],
          16,
        ],
        'fill-extrusion-base': [
          'coalesce',
          ['get', 'render_min_height'],
          ['get', 'min_height'],
          0,
        ],
        'fill-extrusion-opacity': 0.85,
      },
    });
  } catch {
    // ignore
  }
}

function easeInOutCubic(t) {
  return t < 0.5 ? 4 * t * t * t : 1 - (-2 * t + 2) ** 3 / 2;
}

function stopOrbit() {
  orbiting.value = false;
  if (orbitRaf) {
    cancelAnimationFrame(orbitRaf);
    orbitRaf = 0;
  }
}

function startOrbit() {
  const map = mapRef.value;
  if (!map || flying.value || !performanceProfile.enableOrbit || document.hidden) return;
  orbiting.value = true;
  let last = performance.now();
  const tick = (now) => {
    if (!orbiting.value || !mapRef.value || flying.value) return;
    const dt = now - last;
    if (dt < 66) {
      orbitRaf = requestAnimationFrame(tick);
      return;
    }
    last = now;
    map.jumpTo({ bearing: map.getBearing() + Math.min(100, dt) * 0.007 });
    orbitRaf = requestAnimationFrame(tick);
  };
  orbitRaf = requestAnimationFrame(tick);
}

function toggleOrbit() {
  if (orbiting.value) stopOrbit();
  else startOrbit();
}

async function flyToCity(cityName, options = {}) {
  const { instant = false } = options;
  const map = mapRef.value;
  if (!map) return;
  if (!mapStyleReady) {
    pendingFlight = { cityName, options };
    return;
  }

  const dest = findDestination(cityName);
  if (!dest) return;
  const fromDest = findDestination(activeCity.value) || dest;

  const token = ++flyToken;
  if (flyRaf) {
    cancelAnimationFrame(flyRaf);
    flyRaf = 0;
  }
  map.stop();
  activeCity.value = dest.city;
  ensureCityMarkers(map);
  setPrimaryPin(dest.city);
  addPoiMarkers(map, dest);
  ensureFlightSources(map);
  stopOrbit();

  const from = [fromDest.lng, fromDest.lat];
  const to = [dest.lng, dest.lat];
  const same = Math.hypot(from[0] - to[0], from[1] - to[1]) < 0.05;

  const finalCam = {
    center: to,
    zoom: Math.min(dest.zoom, 13.2),
    pitch: Math.min(dest.pitch, performanceProfile.maxPitch),
    bearing: dest.bearing,
  };

  if (instant || same) {
    flying.value = false;
    flyLabel.value = '';
    flyProgress.value = 1;
    setSrc(map, 'flight-path', emptyFc());
    setSrc(map, 'flight-progress', emptyFc());
    setSrc(map, 'flight-plane', emptyFc());
    map.easeTo({
      ...finalCam,
      duration: instant || performanceProfile.reducedMotion ? 0 : 750,
      essential: !performanceProfile.reducedMotion,
    });
    return;
  }

  if (!performanceProfile.animatedFlightPath) {
    flying.value = performanceProfile.flightDuration > 0;
    flyLabel.value = flying.value ? `${fromDest.city} → ${dest.city}` : '';
    flyProgress.value = flying.value ? 0 : 1;
    setSrc(map, 'flight-path', emptyFc());
    setSrc(map, 'flight-progress', emptyFc());
    setSrc(map, 'flight-plane', emptyFc());
    map.flyTo({
      ...finalCam,
      duration: performanceProfile.flightDuration,
      essential: false,
    });
    if (flying.value) {
      map.once('moveend', () => {
        if (token !== flyToken) return;
        flying.value = false;
        flyLabel.value = '';
        flyProgress.value = 1;
      });
    }
    return;
  }

  const arc = buildFlightArc(from, to, performanceProfile.flightSamples, 0.28);
  setSrc(map, 'flight-path', lineFc(arc));
  setSrc(map, 'flight-progress', emptyFc());
  setSrc(map, 'flight-plane', {
    type: 'FeatureCollection',
    features: [{ type: 'Feature', properties: {}, geometry: { type: 'Point', coordinates: from } }],
  });

  flying.value = true;
  flyLabel.value = `${fromDest.city} → ${dest.city} · 准备中`;
  flyProgress.value = 0;

  await prefetchFlightTiles(map.getStyle(), arc, to);
  if (token !== flyToken || !mapRef.value) return;

  flyLabel.value = `${fromDest.city} → ${dest.city}`;

  const duration = performanceProfile.flightDuration;
  const start = performance.now();
  let lastFrame = 0;

  const finishFlight = () => {
    if (token !== flyToken) return;
    flying.value = false;
    flyLabel.value = '';
    flyProgress.value = 1;
    setSrc(map, 'flight-plane', emptyFc());
  };

  // 由 MapLibre 统一计算相机轨迹，避免逐帧 jumpTo 引发瓦片请求抖动。
  map.flyTo({
    ...finalCam,
    duration,
    minZoom: 5.2,
    easing: easeInOutCubic,
    essential: true,
  });
  map.once('moveend', finishFlight);

  const step = (now) => {
    if (token !== flyToken || !mapRef.value) return;
    const raw = Math.min(1, (now - start) / duration);
    if (raw < 1 && now - lastFrame < 66) {
      flyRaf = requestAnimationFrame(step);
      return;
    }
    lastFrame = now;
    const t = easeInOutCubic(raw);
    flyProgress.value = t;

    const { point } = sampleArc(arc, t);
    const progressCoords = arc.slice(0, Math.max(2, Math.floor(t * (arc.length - 1)) + 1));
    progressCoords[progressCoords.length - 1] = point;
    setSrc(map, 'flight-progress', lineFc(progressCoords));
    setSrc(map, 'flight-plane', {
      type: 'FeatureCollection',
      features: [{ type: 'Feature', properties: {}, geometry: { type: 'Point', coordinates: point } }],
    });

    if (raw < 1) {
      flyRaf = requestAnimationFrame(step);
      return;
    }
    flyRaf = 0;
  };

  flyRaf = requestAnimationFrame(step);
}

function resetNorth() {
  mapRef.value?.easeTo({
    bearing: 0,
    pitch: Math.min(50, performanceProfile.maxPitch),
    duration: performanceProfile.reducedMotion ? 0 : 500,
  });
}

function pitchUp() {
  const map = mapRef.value;
  if (!map) return;
  map.easeTo({
    pitch: Math.min(performanceProfile.maxPitch, map.getPitch() + 12),
    duration: performanceProfile.reducedMotion ? 0 : 260,
  });
}

function zoomIn() {
  mapRef.value?.zoomIn({ duration: 250 });
}

function zoomOut() {
  mapRef.value?.zoomOut({ duration: 250 });
}

function selectCity(event) {
  const city = event.target.value;
  if (!city || city === activeCity.value) return;
  flyToCity(city);
  emit('city-change', city);
}

onMounted(async () => {
  if (!containerRef.value || destroyed) return;
  document.addEventListener('visibilitychange', handleVisibilityChange);
  status.value = 'loading';
  mapRevealed.value = false;
  loadPhase.value = '预连接与拉取样式…';
  preconnectMapCdn();

  try {
    const [style, mlMod] = await Promise.all([
      resolveMapStyle(),
      prefetchMapLibre(),
    ]);
    if (destroyed) return;

    maplibregl = mlMod.default || mlMod;
    loadPhase.value = '下载地图瓦片…';

    const dest = findDestination(props.city);
    activeCity.value = dest.city;
    const initialPitch = Math.min(dest.pitch, performanceProfile.maxPitch);

    // 直接落在最终机位，等该视角瓦片全部就绪再展示
    const map = new maplibregl.Map({
      container: containerRef.value,
      style,
      center: [dest.lng, dest.lat],
      zoom: Math.min(dest.zoom, 12.8),
      pitch: initialPitch,
      bearing: dest.bearing,
      antialias: performanceProfile.antialias,
      pixelRatio: performanceProfile.pixelRatio,
      attributionControl: { compact: true },
      interactive: false, // 渲染完前禁止拖动，避免半屏状态
      maxPitch: performanceProfile.maxPitch,
      fadeDuration: 0,
      maxTileCacheSize: performanceProfile.maxTileCacheSize,
      cancelPendingTileRequestsWhileZooming: performanceProfile.lowPower,
      renderWorldCopies: false,
      localIdeographFontFamily: '"Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif',
      collectResourceTiming: false,
      crossSourceCollisions: false,
    });

    map.getCanvas().style.background = MAP_THEME.land;
    if (!props.compact && !performanceProfile.mobile) {
      map.addControl(new maplibregl.ScaleControl({ maxWidth: 100 }), 'bottom-left');
    }
    mapRef.value = map;

    const reveal = () => {
      if (destroyed || !mapRef.value || mapRevealed.value) return;
      if (revealTimer) window.clearTimeout(revealTimer);
      map.resize();
      status.value = 'ready';
      mapRevealed.value = true;
      loadPhase.value = '';
      // 恢复交互
      if (props.interactive) {
        map.dragPan.enable();
        map.scrollZoom.enable();
        map.boxZoom.enable();
        map.dragRotate.enable();
        map.keyboard.enable();
        map.doubleClickZoom.enable();
        map.touchZoomRotate.enable();
      }
      emit('ready', map);
      if (props.autoOrbit && performanceProfile.enableOrbit) startOrbit();
    };

    const enrichMap = () => {
      if (destroyed || !mapRef.value || !map.isStyleLoaded()) return;
      ensureFlightSources(map);
      ensureCityMarkers(map);
      addPoiMarkers(map, dest);
      ensurePublicSources(map);
      ensureTrackSources(map);
      tryEnable3dBuildings(map);
    };

    const scheduleEnrichment = () => {
      const run = () => requestAnimationFrame(enrichMap);
      if ('requestIdleCallback' in window && !performanceProfile.lowPower) {
        enrichIdle = window.requestIdleCallback(run, { timeout: 1200 });
      } else {
        enrichTimer = window.setTimeout(run, performanceProfile.mobile ? 450 : 180);
      }
    };

    map.once('load', () => {
      if (destroyed) return;
      mapStyleReady = true;
      loadPhase.value = '地图首帧就绪…';
      requestAnimationFrame(() => requestAnimationFrame(() => {
        reveal();
        scheduleEnrichment();
        if (pendingFlight) {
          const nextFlight = pendingFlight;
          pendingFlight = null;
          flyToCity(nextFlight.cityName, nextFlight.options);
        }
      }));
    });

    // 样式或单张瓦片很慢时也先展示底色和已到达的内容。
    revealTimer = window.setTimeout(() => {
      if (!mapRevealed.value && status.value === 'loading') {
        loadPhase.value = '网络较慢，先显示已加载内容…';
        reveal();
      }
    }, 3800);

    map.on('error', (e) => {
      if (status.value === 'loading' && e?.error?.message && !mapRevealed.value) {
        // 单瓦片错误不中断，只记日志
        console.warn('[map] tile/style error', e.error?.message);
      }
    });

    map.on('dragstart', stopOrbit);
    map.on('mousedown', stopOrbit);
    map.on('touchstart', stopOrbit);
    map.on('click', addTrackPoint);

    resizeObserver = new ResizeObserver(() => {
      if (resizeRaf) cancelAnimationFrame(resizeRaf);
      resizeRaf = requestAnimationFrame(() => map.resize());
    });
    resizeObserver.observe(containerRef.value);
  } catch (err) {
    if (destroyed) return;
    status.value = 'error';
    errorMsg.value = err?.message || '地图加载失败';
    emit('error', err);
  }
});

onUnmounted(() => {
  destroyed = true;
  stopOrbit();
  flyToken += 1;
  pendingFlight = null;
  if (flyRaf) cancelAnimationFrame(flyRaf);
  if (trackRaf) cancelAnimationFrame(trackRaf);
  if (resizeRaf) cancelAnimationFrame(resizeRaf);
  if (revealTimer) window.clearTimeout(revealTimer);
  if (enrichTimer) window.clearTimeout(enrichTimer);
  if (enrichIdle && 'cancelIdleCallback' in window) window.cancelIdleCallback(enrichIdle);
  clearPois();
  clearPublicMarkers();
  clearTrackMarkers();
  cityMarkers.forEach((m) => m.remove());
  cityMarkers.clear();
  resizeObserver?.disconnect();
  mapRef.value?.remove();
  mapRef.value = null;
  document.removeEventListener('visibilitychange', handleVisibilityChange);
});

function handleVisibilityChange() {
  if (document.hidden) stopOrbit();
}

watch(
  () => props.city,
  (city) => {
    if (!city || status.value !== 'ready') return;
    // 点击城市后父组件会回传同一个 prop，不应重启或取消当前飞行。
    if (city === activeCity.value) return;
    flyToCity(city);
  },
);

watch(
  () => props.publicData,
  () => updatePublicSources(),
  { deep: true },
);

watch(() => props.selectedPlaceIds, syncPublicMarkerStates, { deep: true });

watch(() => props.trackEditor, (enabled) => {
  if (enabled) ensureTrackSources(mapRef.value);
  else {
    trackEditing.value = false;
    syncTrackCursor();
  }
});

watch(() => props.initialTrackPoints, (nodes) => {
  const next = normalizeTrackPoints(nodes);
  trackPoints.value = next;
  selectedTrackIndex.value = -1;
  ensureTrackSources(mapRef.value);
  renderTrack();
}, { deep: true });

defineExpose({ flyToCity, flyToPoint, toggleOrbit, stopOrbit, clearTrack, getTrackIntent: trackIntent });
</script>

<template>
  <div
    class="map3d map3d--themed map3d--maplibre"
    :class="{
      'map3d--compact': compact,
      'map3d--flying': flying,
      'map3d--waiting': !mapRevealed && status !== 'error',
      'map3d--revealed': mapRevealed,
      'map3d--track-editing': trackEditing,
    }"
    :style="{ height, '--map-land': MAP_THEME.land, '--map-water': MAP_THEME.water }"
  >
    <!-- 未就绪时 canvas 透明，后台照常渲染 -->
    <div
      ref="containerRef"
      class="map3d-canvas"
      :class="{ 'is-hidden': !mapRevealed }"
      :aria-hidden="(!mapRevealed).toString()"
    />

    <!-- 全屏等待：瓦片全部画完再淡出 -->
    <Transition name="map-fade">
      <div v-if="!mapRevealed && status !== 'error'" class="map3d-overlay map3d-loading map3d-loading--full">
        <div class="map3d-load-card">
          <div class="map-spinner" />
          <strong>地图渲染中</strong>
          <p>{{ loadPhase || '请稍候，画面准备好后再显示' }}</p>
          <div class="map3d-loadbar map3d-loadbar--card" aria-hidden="true">
            <i />
          </div>
        </div>
      </div>
    </Transition>

    <div v-if="status === 'error'" class="map3d-overlay map3d-error">
      <strong>地图加载失败</strong>
      <p>{{ errorMsg || '请检查网络后刷新' }}</p>
    </div>

    <Transition name="map-fade">
      <div v-if="flying" class="map3d-flyveil" aria-live="polite">
        <div class="map3d-flypill">
          <span class="map3d-flydot" />
          <span>{{ flyLabel || '飞行中' }}</span>
          <span class="map3d-flypct">{{ Math.round(flyProgress * 100) }}%</span>
        </div>
        <div class="map3d-flybar">
          <i :style="{ width: `${Math.round(flyProgress * 100)}%` }" />
        </div>
      </div>
    </Transition>

    <div v-show="mapRevealed" class="map3d-hud">
      <label class="map3d-city-picker">
        <span>目的地</span>
        <select
          :value="activeCity"
          aria-label="选择地图目的地"
          :disabled="flying"
          @change="selectCity"
        >
          <option
            v-for="item in mapDestinations"
            :key="item.city"
            :value="item.city"
          >
            {{ item.city }} · {{ item.province }}
          </option>
        </select>
      </label>
      <div class="map3d-tools">
        <button type="button" class="map-tool-btn" title="拉近" aria-label="拉近" @click="zoomIn"><ZoomIn :size="18" :stroke-width="2" /></button>
        <button type="button" class="map-tool-btn" title="拉远" aria-label="拉远" @click="zoomOut"><ZoomOut :size="18" :stroke-width="2" /></button>
        <button type="button" class="map-tool-btn" title="3D" @click="pitchUp">3D</button>
        <button type="button" class="map-tool-btn" title="指北" @click="resetNorth">北</button>
        <button
          type="button"
          class="map-tool-btn"
          :class="{ 'is-on': orbiting }"
          title="环绕"
          :disabled="flying || !performanceProfile.enableOrbit"
          @click="toggleOrbit"
        >
          环绕
        </button>
      </div>
    </div>

    <section
      v-if="mapRevealed && trackEditor"
      class="track-editor-hud"
      :class="{ 'is-active': trackEditing, 'has-route': trackPoints.length > 1 }"
      aria-label="地图轨迹编辑器"
    >
      <header>
        <span class="track-editor-status"><i />路线草稿</span>
        <strong>{{ trackPoints.length }} 节点<span v-if="trackDistance"> · {{ trackDistance.toFixed(1) }} km</span></strong>
      </header>
      <p aria-live="polite">
        <template v-if="trackPoints.length >= MAX_TRACK_NODES">节点已满，撤销或清空后继续</template>
        <template v-else-if="trackEditing && lastTrackPoint?.name">已吸附「{{ lastTrackPoint.name }}」· 下一枚 #{{ String(trackPoints.length + 1).padStart(2, '0') }}</template>
        <template v-else-if="trackEditing">点击地图，放下 #{{ String(trackPoints.length + 1).padStart(2, '0') }}</template>
        <template v-else-if="lastTrackPoint?.name">路线停在「{{ lastTrackPoint.name }}」</template>
        <template v-else-if="trackPoints.length">继续落点，或先调整地图视角</template>
        <template v-else>逐点画出你想走的路线</template>
      </p>
      <section v-if="selectedTrackPoint" class="track-node-editor" aria-label="编辑当前路线节点">
        <header>
          <b>#{{ String(selectedTrackPoint.number).padStart(2, '0') }}</b>
          <strong>{{ selectedTrackPoint.name || `自定义节点 ${selectedTrackPoint.number}` }}</strong>
          <button type="button" aria-label="关闭节点编辑" @click="selectedTrackIndex = -1"><X :size="16" :stroke-width="2" /></button>
        </header>
        <textarea
          :value="selectedTrackPoint.note || ''"
          maxlength="240"
          rows="2"
          :aria-label="`节点 ${selectedTrackPoint.number} 备注`"
          placeholder="补一句：想看日落、不要太赶、需要预约…"
          @input="updateSelectedTrackPoint({ note: $event.target.value })"
        />
        <div class="track-node-preferences" role="group" aria-label="节点偏好">
          <button
            v-for="preference in NODE_PREFERENCES"
            :key="preference"
            type="button"
            :class="{ 'is-on': selectedTrackPoint.preferences?.includes(preference) }"
            :aria-pressed="selectedTrackPoint.preferences?.includes(preference)"
            @click="toggleNodePreference(preference)"
          >{{ preference }}</button>
        </div>
        <footer>
          <button type="button" :disabled="selectedTrackIndex === 0" @click="moveSelectedTrackPoint(-1)"><ArrowLeft :size="15" :stroke-width="2.2" /> 前移</button>
          <button type="button" :disabled="selectedTrackIndex === trackPoints.length - 1" @click="moveSelectedTrackPoint(1)">后移 <ArrowRight :size="15" :stroke-width="2.2" /></button>
          <button type="button" class="is-danger" @click="removeSelectedTrackPoint">删除节点</button>
        </footer>
      </section>
      <div class="track-editor-actions">
        <button
          type="button"
          class="track-editor-primary"
          :class="{ 'is-on': trackEditing }"
          :aria-pressed="trackEditing"
          :disabled="trackPoints.length >= MAX_TRACK_NODES"
          @click="toggleTrackEditing"
        >
          <span>{{ trackEditing ? '暂停落点' : trackPoints.length ? '继续画路线' : '开始画路线' }}</span>
          <b aria-hidden="true"><component :is="trackEditing ? Pause : Plus" :size="15" :stroke-width="2.2" /></b>
        </button>
        <button type="button" :disabled="!trackPoints.length" aria-label="撤销最后一个节点" @click="undoTrackPoint"><Undo2 :size="15" :stroke-width="2.2" /></button>
        <button type="button" :disabled="!trackPoints.length" aria-label="清空路线" @click="clearTrack">清空</button>
      </div>
      <button v-if="trackPoints.length > 1" type="button" class="track-editor-plan" @click="planTrack">
        用这条路线规划
        <span aria-hidden="true">{{ String(trackPoints.length).padStart(2, '0') }} <ArrowRight :size="15" :stroke-width="2.2" /> AI</span>
      </button>
    </section>

    <div v-if="mapRevealed" class="map3d-legend">
      <span class="legend-dot" />
      <span>MapLibre · {{ performanceProfile.enable3d ? '3D' : '轻量模式' }} · 航线</span>
    </div>
  </div>
</template>

<style>
.track-editor-hud {
  position: absolute;
  right: 52px;
  bottom: 14px;
  z-index: 6;
  width: min(420px, calc(100% - 28px));
  padding: 18px;
  border: 1px solid var(--tm-line-strong);
  border-top: 3px solid var(--tm-accent);
  border-radius: var(--tm-radius-panel);
  background: rgba(21, 17, 12, 0.94);
  color: var(--tm-ink);
  box-shadow: var(--tm-shadow-lift);
  backdrop-filter: blur(22px) saturate(1.4);
  -webkit-backdrop-filter: blur(22px) saturate(1.4);
  transition: transform .3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow .3s ease;
}

.track-editor-hud.is-active {
  box-shadow: var(--tm-shadow-lift), 0 0 0 3px var(--tm-accent-soft);
  transform: translateY(-2px);
  border-top-color: var(--tm-accent);
}

.track-editor-hud header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.track-editor-hud header strong { font-size: 14px; color: var(--tm-ink); }
.track-editor-status { display: inline-flex; align-items: center; gap: 8px; color: var(--tm-accent); font: 800 11px/1 var(--font-mono); letter-spacing: .16em; text-transform: uppercase; }
.track-editor-status i { width: 7px; height: 7px; border-radius: 50%; background: var(--tm-accent); box-shadow: 0 0 0 4px var(--tm-accent-soft); }
.track-editor-hud.is-active .track-editor-status i { background: var(--tm-accent); box-shadow: 0 0 0 5px var(--tm-accent-soft); animation: track-status-pulse 1.2s ease-in-out infinite; }
.track-editor-hud p { margin: 11px 0 14px; color: var(--tm-ink-soft); font-size: 13px; line-height: 1.5; }
.track-node-editor { display: grid; gap: 12px; margin: 0 0 14px; padding: 14px; border: 1px solid var(--tm-line); border-radius: var(--tm-radius-control); background: var(--tm-paper-muted); }
.track-node-editor header { justify-content: start; }
.track-node-editor header b { display: grid; width: 32px; height: 32px; place-items: center; border-radius: 50%; background: var(--tm-accent); color: #160d05; font: 900 11px/1 var(--font-mono); }
.track-node-editor header strong { overflow: hidden; flex: 1; text-overflow: ellipsis; white-space: nowrap; color: var(--tm-ink); }
.track-node-editor header button { border: 0; background: transparent; color: var(--tm-muted); cursor: pointer; font-size: 0; display: inline-flex; align-items: center; }
.track-node-editor header button svg { width: 16px; height: 16px; }
.track-node-editor textarea { width: 100%; min-height: 76px; resize: vertical; padding: 10px 11px; border: 1px solid var(--tm-line-strong); border-radius: var(--tm-radius-control); background: var(--tm-canvas-2); color: var(--tm-ink); font: 500 13px/1.5 var(--font-body); }
.track-node-editor textarea::placeholder { color: var(--tm-muted-soft); }
.track-node-editor textarea:focus { border-color: var(--tm-accent); outline: 2px solid var(--tm-accent-soft); background: var(--tm-paper); }
.track-node-preferences { display: flex; flex-wrap: wrap; gap: 7px; }
.track-node-preferences button { padding: 6px 9px; border: 1px solid var(--tm-line-strong); border-radius: var(--tm-radius-pill); background: var(--tm-paper-muted); color: var(--tm-ink-soft); cursor: pointer; font-size: 11px; font-weight: 700; transition: all .2s ease; }
.track-node-preferences button:hover { border-color: var(--tm-accent); color: var(--tm-accent); }
.track-node-preferences button.is-on { border-color: var(--tm-accent); background: var(--tm-accent); color: #160d05; }
.track-node-editor footer { display: grid; grid-template-columns: 1fr 1fr auto; gap: 7px; }
.track-node-editor footer button { min-height: 36px; padding: 0 10px; border: 1px solid var(--tm-line-strong); border-radius: var(--tm-radius-control); background: var(--tm-paper-muted); color: var(--tm-ink); cursor: pointer; font-size: 11px; font-weight: 700; display: inline-flex; align-items: center; justify-content: center; gap: 4px; transition: all .2s ease; }
.track-node-editor footer button svg { width: 14px; height: 14px; }
.track-node-editor footer button:hover:not(:disabled) { border-color: var(--tm-accent); color: var(--tm-accent); }
.track-node-editor footer button:disabled { cursor: not-allowed; opacity: .32; }
.track-node-editor footer .is-danger { color: var(--tm-danger); }
.track-node-editor footer .is-danger:hover:not(:disabled) { border-color: var(--tm-danger); color: var(--tm-danger); background: var(--tm-danger-soft); }

.track-editor-actions { display: grid; grid-template-columns: minmax(0, 1fr) 46px 58px; gap: 9px; }
.track-editor-actions button { min-height: 46px; padding: 0 12px; border: 1px solid var(--tm-line-strong); border-radius: var(--tm-radius-button); background: var(--tm-paper-muted); color: var(--tm-ink); cursor: pointer; font-size: 13px; font-weight: 700; display: inline-flex; align-items: center; justify-content: center; gap: 6px; transition: all .2s ease; }
.track-editor-actions button svg { width: 15px; height: 15px; }
.track-editor-actions button:hover:not(:disabled),
.track-editor-actions button:focus-visible { border-color: var(--tm-accent); color: var(--tm-accent); background: var(--tm-paper-raised); }
.track-editor-actions button:disabled { cursor: not-allowed; opacity: .36; }
.track-editor-actions .track-editor-primary { display: flex; align-items: center; justify-content: space-between; border-color: var(--tm-accent); background: var(--tm-accent-soft); color: var(--tm-accent); }
.track-editor-actions .track-editor-primary b { display: grid; width: 28px; height: 28px; place-items: center; border-radius: var(--tm-radius-control); background: var(--tm-accent); color: #160d05; font-size: 0; }
.track-editor-actions .track-editor-primary b svg { width: 15px; height: 15px; }
.track-editor-actions .track-editor-primary.is-on { background: var(--tm-accent); color: #160d05; }
.track-editor-actions .track-editor-primary.is-on b { background: rgba(22, 13, 5, 0.25); color: #160d05; }
.track-editor-plan {
  display: flex;
  width: 100%;
  min-height: 48px;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding: 0 15px;
  border: 0;
  border-radius: var(--tm-radius-button);
  background: var(--tm-accent);
  color: #160d05;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 14px 30px -12px var(--tm-accent-glow);
  transition: transform .3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow .25s ease, background .25s ease;
}
.track-editor-plan span { font: 700 11px/1 var(--font-mono); letter-spacing: .12em; display: inline-flex; align-items: center; gap: 4px; }
.track-editor-plan span svg { width: 14px; height: 14px; }
.track-editor-plan:hover,
.track-editor-plan:focus-visible { background: var(--tm-accent-hover); transform: translateY(-2px); box-shadow: 0 20px 40px -12px var(--tm-accent-glow); }

.track-node {
  position: relative;
  display: grid;
  width: 31px;
  height: 31px;
  place-items: center;
  border: 3px solid var(--tm-canvas);
  border-radius: 50% 50% 50% 8px;
  background: var(--tm-paper-raised);
  color: var(--tm-ink);
  box-shadow: 0 5px 13px rgba(0, 0, 0, 0.5);
  transform: rotate(-7deg);
}
.track-node span { font: 900 11px/1 var(--font-mono); transform: rotate(7deg); }
.track-node em {
  position: absolute;
  left: 34px;
  display: none;
  width: max-content;
  max-width: 150px;
  overflow: hidden;
  padding: 5px 8px;
  border: 1px solid var(--tm-line-strong);
  border-radius: var(--tm-radius-control);
  background: rgba(21, 17, 12, 0.95);
  color: var(--tm-ink);
  font: 700 10px/1.2 var(--font-body);
  text-overflow: ellipsis;
  white-space: nowrap;
  box-shadow: var(--tm-shadow-card);
  backdrop-filter: blur(12px);
  transform: rotate(7deg);
}
.track-node.is-latest em { display: block; }
.track-node.label-left em { right: 34px; left: auto; }
.track-node.is-poi { box-shadow: 0 5px 13px rgba(0, 0, 0, 0.5), 0 0 0 4px var(--tm-accent-soft); }
.track-node.is-selected { outline: 3px solid var(--tm-accent); outline-offset: 3px; }
.track-node.is-latest { background: var(--tm-accent); color: #160d05; animation: track-node-drop .34s cubic-bezier(.2, .9, .28, 1.4); }
.track-node.is-latest::after { position: absolute; inset: -8px; border: 2px solid var(--tm-accent-glow); border-radius: 50%; content: ''; animation: track-node-ring .6s ease-out both; }

@keyframes track-node-drop {
  from { opacity: 0; transform: translateY(-20px) rotate(-7deg) scale(.5); }
  to { opacity: 1; transform: translateY(0) rotate(-7deg) scale(1); }
}
@keyframes track-node-ring {
  from { opacity: .9; transform: scale(.5); }
  to { opacity: 0; transform: scale(1.35); }
}
@keyframes track-status-pulse { 50% { transform: scale(1.35); } }

.public-map-marker {
  width: 42px;
  height: 48px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--tm-ink);
  cursor: pointer;
  filter: drop-shadow(0 6px 8px rgba(0, 0, 0, 0.5));
}

.public-map-marker > span {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 3px solid var(--tm-canvas);
  border-radius: 16px 16px 16px 5px;
  background: var(--tm-sun);
  color: #160d05;
  transform: rotate(-4deg);
  transition: transform .16s ease;
}

.public-map-marker svg { width: 20px; height: 20px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; transform: rotate(4deg); }
.public-map-marker--hotel > span { border-radius: 8px 18px 8px 18px; background: var(--tm-success); color: #0c0a08; transform: rotate(2deg); }
.public-map-marker--hotel svg { transform: rotate(-2deg); }
.public-map-marker--restaurant > span { border-radius: 50% 50% 12px 50%; background: var(--tm-accent); color: #160d05; transform: rotate(6deg); }
.public-map-marker--restaurant svg { transform: rotate(-6deg); }
.public-map-marker--airport > span { border-radius: 50% 8px 50% 8px; background: var(--tm-sun); color: #160d05; transform: rotate(-3deg); }
.public-map-marker--airport svg { transform: rotate(3deg); }
.public-map-marker:hover > span,
.public-map-marker:focus-visible > span,
.public-map-marker.is-selected > span { transform: translateY(-4px) scale(1.08); }
.public-map-marker.is-selected > span { outline: 3px solid var(--tm-accent); }
.public-map-marker.is-in-track > span {
  outline: 3px solid var(--tm-accent);
  outline-offset: 3px;
  transform: translateY(-3px) scale(1.06);
}
.public-map-marker:focus-visible { outline: 2px solid var(--tm-accent); outline-offset: 3px; border-radius: 12px; }

.travel-node-popup { pointer-events: none; }
.travel-node-popup .maplibregl-popup-content {
  width: min(340px, 78vw);
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--tm-line-strong);
  border-radius: var(--tm-radius-panel);
  background: rgba(21, 17, 12, 0.97);
  color: var(--tm-ink);
  box-shadow: var(--tm-shadow-lift);
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
}
.travel-node-popup.maplibregl-popup-anchor-bottom-left .maplibregl-popup-tip,
.travel-node-popup.maplibregl-popup-anchor-bottom-right .maplibregl-popup-tip { border-top-color: rgba(21, 17, 12, 0.97); }
.travel-node-popup.maplibregl-popup-anchor-top-left .maplibregl-popup-tip,
.travel-node-popup.maplibregl-popup-anchor-top-right .maplibregl-popup-tip { border-bottom-color: rgba(21, 17, 12, 0.97); }
.travel-node-visual { position: relative; display: grid; height: 108px; overflow: hidden; place-items: center; background: linear-gradient(135deg, var(--tm-accent-soft), rgba(232, 93, 31, 0.3)); color: var(--tm-accent); }
.travel-node-visual::after { position: absolute; inset: auto 0 0; height: 46%; background: linear-gradient(transparent, rgba(0, 0, 0, 0.3)); content: ''; }
.travel-node-visual > img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.travel-node-visual > span { display: grid; width: 42px; height: 42px; place-items: center; }
.travel-node-visual svg { width: 38px; height: 38px; fill: currentColor; }
.travel-node-body { display: grid; gap: 9px; padding: 13px 15px 15px; }
.travel-node-body > span { color: var(--tm-accent); font: 700 10px/1.3 var(--font-mono); letter-spacing: .16em; text-transform: uppercase; }
.travel-node-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.travel-node-heading > strong { font: 700 16px/1.3 var(--font-display); color: var(--tm-ink); }
.travel-node-heading > b { flex: 0 0 auto; padding: 5px 7px; border-radius: var(--tm-radius-control); background: var(--tm-accent-soft); color: var(--tm-accent); font: 900 11px/1 var(--font-mono); }
.travel-node-facts { display: flex; flex-wrap: wrap; gap: 6px; }
.travel-node-facts > span { padding: 5px 7px; border: 1px solid var(--tm-line); border-radius: var(--tm-radius-control); background: var(--tm-paper-muted); color: var(--tm-ink-soft); font: 600 10px/1.2 var(--font-body); }
.travel-node-body > p { margin: 0; color: var(--tm-muted); font-size: 11px; line-height: 1.55; }
.travel-node-body > small { padding: 9px 10px; border-left: 3px solid var(--tm-accent); border-radius: 0 var(--tm-radius-control) var(--tm-radius-control) 0; background: var(--tm-accent-soft); color: var(--tm-accent); font-size: 10px; line-height: 1.45; }

@media (prefers-reduced-motion: reduce) {
  .public-map-marker > span { transition: none; }
  .track-editor-hud,
  .track-node,
  .track-node::after,
  .track-editor-status i { animation: none !important; transition: none; }
}

@media (max-width: 640px) {
  .track-editor-hud { right: 10px; bottom: 78px; width: calc(100% - 20px); }
}
</style>
