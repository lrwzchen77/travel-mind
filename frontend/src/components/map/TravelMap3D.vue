<script setup>
/**
 * MapLibre + OpenFreeMap
 * 加载优化：样式预取并行、直达目标城市、idle 后再加 3D/标注、可见即用
 */
import { onMounted, onUnmounted, ref, shallowRef, watch } from 'vue';
import { findDestination, geoDestinations } from '../../data/geoDestinations.js';
import { buildFlightArc, sampleArc } from '../../map/flightPath.js';
import { emptyFc, lineFc, MAP_THEME } from '../../map/travelMindStyle.js';
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
});

const emit = defineEmits(['city-change', 'ready', 'error']);

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
/** 仅当首屏瓦片全部渲染完才为 true，避免半成品露出来 */
const mapRevealed = ref(false);

/** @type {Map<string, import('maplibre-gl').Marker>} */
const cityMarkers = new Map();
/** @type {import('maplibre-gl').Marker[]} */
const poiMarkers = [];

let maplibregl = null;
let orbitRaf = 0;
let flyRaf = 0;
let flyToken = 0;
let resizeObserver;
let destroyed = false;

function setPrimaryPin(city) {
  cityMarkers.forEach((marker, name) => {
    marker.getElement().classList.toggle('map-pin--primary', name === city);
  });
}

function ensureCityMarkers(map) {
  if (cityMarkers.size || !maplibregl) return;
  geoDestinations.forEach((dest) => {
    const el = document.createElement('button');
    el.type = 'button';
    el.className = 'map-pin';
    el.innerHTML = `<span class="map-pin-dot"></span><span class="map-pin-label">${dest.city}</span>`;
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

function clearPois() {
  while (poiMarkers.length) poiMarkers.pop().remove();
}

function addPoiMarkers(map, dest) {
  clearPois();
  if (!props.showPois || !dest.pois?.length || !maplibregl) return;
  dest.pois.forEach((poi) => {
    const el = document.createElement('div');
    el.className = 'map-poi';
    el.innerHTML = `<span></span><em>${poi.name}</em>`;
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
  if (map.getLayer('tm-3d-buildings')) return;
  const style = map.getStyle();
  if (!style?.sources) return;
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
  if (!map || flying.value) return;
  orbiting.value = true;
  let last = performance.now();
  const tick = (now) => {
    if (!orbiting.value || !mapRef.value || flying.value) return;
    const dt = Math.min(32, now - last);
    last = now;
    map.easeTo({ bearing: map.getBearing() + dt * 0.007, duration: 0, essential: true });
    orbitRaf = requestAnimationFrame(tick);
  };
  orbitRaf = requestAnimationFrame(tick);
}

function toggleOrbit() {
  if (orbiting.value) stopOrbit();
  else startOrbit();
}

function flyToCity(cityName, options = {}) {
  const { instant = false } = options;
  const map = mapRef.value;
  if (!map || !map.isStyleLoaded()) return;

  const dest = findDestination(cityName);
  if (!dest) return;
  const fromDest = findDestination(activeCity.value) || dest;

  const token = ++flyToken;
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
    pitch: dest.pitch,
    bearing: dest.bearing,
  };

  if (instant || same) {
    flying.value = false;
    flyLabel.value = '';
    flyProgress.value = 1;
    setSrc(map, 'flight-path', emptyFc());
    setSrc(map, 'flight-progress', emptyFc());
    setSrc(map, 'flight-plane', emptyFc());
    map.easeTo({ ...finalCam, duration: instant ? 0 : 1200, essential: true });
    return;
  }

  const arc = buildFlightArc(from, to, 96, 0.28);
  setSrc(map, 'flight-path', lineFc(arc));
  setSrc(map, 'flight-progress', emptyFc());
  setSrc(map, 'flight-plane', {
    type: 'FeatureCollection',
    features: [{ type: 'Feature', properties: {}, geometry: { type: 'Point', coordinates: from } }],
  });

  flying.value = true;
  flyLabel.value = `${fromDest.city} → ${dest.city}`;
  flyProgress.value = 0;

  const duration = 5200;
  const start = performance.now();
  if (flyRaf) cancelAnimationFrame(flyRaf);

  const step = (now) => {
    if (token !== flyToken || !mapRef.value) return;
    const raw = Math.min(1, (now - start) / duration);
    const t = easeInOutCubic(raw);
    flyProgress.value = t;

    const { point, bearing } = sampleArc(arc, t);
    const progressCoords = arc.slice(0, Math.max(2, Math.floor(t * (arc.length - 1)) + 1));
    progressCoords[progressCoords.length - 1] = point;
    setSrc(map, 'flight-progress', lineFc(progressCoords));
    setSrc(map, 'flight-plane', {
      type: 'FeatureCollection',
      features: [{ type: 'Feature', properties: {}, geometry: { type: 'Point', coordinates: point } }],
    });

    const midBoost = Math.sin(t * Math.PI);
    const zoom = 5.4 + (finalCam.zoom - 5.4) * (0.15 + 0.85 * t * t) - midBoost * 1.0;
    const pitch = 35 + (finalCam.pitch - 35) * t + midBoost * 12;
    const camBearing = bearing * 0.35 + finalCam.bearing * t * 0.65;

    map.jumpTo({
      center: point,
      zoom: Math.max(4.8, zoom),
      pitch: Math.min(68, pitch),
      bearing: camBearing,
    });

    if (raw < 1) {
      flyRaf = requestAnimationFrame(step);
      return;
    }

    map.easeTo({ ...finalCam, duration: 900, essential: true });
    map.once('moveend', () => {
      if (token !== flyToken) return;
      flying.value = false;
      flyLabel.value = '';
      flyProgress.value = 1;
      setSrc(map, 'flight-plane', emptyFc());
    });
  };

  flyRaf = requestAnimationFrame(step);
}

function resetNorth() {
  mapRef.value?.easeTo({ bearing: 0, pitch: 55, duration: 700 });
}

function pitchUp() {
  const map = mapRef.value;
  if (!map) return;
  map.easeTo({ pitch: Math.min(70, map.getPitch() + 12), duration: 320 });
}

function zoomIn() {
  mapRef.value?.zoomIn({ duration: 250 });
}

function zoomOut() {
  mapRef.value?.zoomOut({ duration: 250 });
}

onMounted(async () => {
  if (!containerRef.value || destroyed) return;
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

    // 直接落在最终机位，等该视角瓦片全部就绪再展示
    const map = new maplibregl.Map({
      container: containerRef.value,
      style,
      center: [dest.lng, dest.lat],
      zoom: Math.min(dest.zoom, 12.8),
      pitch: dest.pitch,
      bearing: dest.bearing,
      antialias: true,
      attributionControl: { compact: true },
      interactive: false, // 渲染完前禁止拖动，避免半屏状态
      maxPitch: 75,
      fadeDuration: 0,
      maxTileCacheSize: 512,
      renderWorldCopies: false,
      localIdeographFontFamily: '"Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif',
      collectResourceTiming: false,
      crossSourceCollisions: false,
    });

    map.getCanvas().style.background = MAP_THEME.land;
    map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), 'bottom-right');
    map.addControl(new maplibregl.ScaleControl({ maxWidth: 100 }), 'bottom-left');
    mapRef.value = map;

    const reveal = () => {
      if (destroyed || !mapRef.value || mapRevealed.value) return;
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
      if (props.autoOrbit) startOrbit();
    };

    map.once('load', () => {
      if (destroyed) return;
      loadPhase.value = '绘制标注与立体建筑…';
      ensureFlightSources(map);
      ensureCityMarkers(map);
      addPoiMarkers(map, dest);
      tryEnable3dBuildings(map);

      // 再 jump 一次保证最终机位，然后等 idle = 瓦片+标注都画完
      map.jumpTo({
        center: [dest.lng, dest.lat],
        zoom: Math.min(dest.zoom, 12.8),
        pitch: dest.pitch,
        bearing: dest.bearing,
      });

      loadPhase.value = '等待画面渲染完成…';

      // idle：当前视角没有未完成的瓦片请求与重绘
      const onIdle = () => {
        if (destroyed) return;
        // 再等 1～2 帧，确保 WebGL 画完
        requestAnimationFrame(() => {
          requestAnimationFrame(reveal);
        });
      };

      map.once('idle', onIdle);

      // 兜底：极慢网络最长等 12s 也展示，避免永久转圈
      window.setTimeout(() => {
        if (!mapRevealed.value && status.value === 'loading') {
          loadPhase.value = '网络较慢，即将显示…';
          reveal();
        }
      }, 12000);
    });

    map.on('error', (e) => {
      if (status.value === 'loading' && e?.error?.message && !mapRevealed.value) {
        // 单瓦片错误不中断，只记日志
        console.warn('[map] tile/style error', e.error?.message);
      }
    });

    map.on('dragstart', stopOrbit);
    map.on('mousedown', stopOrbit);
    map.on('touchstart', stopOrbit);

    resizeObserver = new ResizeObserver(() => {
      map.resize();
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
  if (flyRaf) cancelAnimationFrame(flyRaf);
  clearPois();
  cityMarkers.forEach((m) => m.remove());
  cityMarkers.clear();
  resizeObserver?.disconnect();
  mapRef.value?.remove();
  mapRef.value = null;
});

watch(
  () => props.city,
  (city) => {
    if (!city || status.value !== 'ready') return;
    if (city === activeCity.value && !flying.value) return;
    flyToCity(city);
  },
);

defineExpose({ flyToCity, toggleOrbit, stopOrbit });
</script>

<template>
  <div
    class="map3d map3d--themed map3d--maplibre"
    :class="{
      'map3d--compact': compact,
      'map3d--flying': flying,
      'map3d--waiting': !mapRevealed && status !== 'error',
      'map3d--revealed': mapRevealed,
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
      <div class="map3d-chip-row">
        <button
          v-for="item in geoDestinations"
          :key="item.city"
          type="button"
          class="map-city-chip"
          :class="{ 'is-on': activeCity === item.city }"
          @click="flyToCity(item.city); $emit('city-change', item.city)"
        >
          {{ item.city }}
        </button>
      </div>
      <div class="map3d-tools">
        <button type="button" class="map-tool-btn" title="拉近" @click="zoomIn">＋</button>
        <button type="button" class="map-tool-btn" title="拉远" @click="zoomOut">－</button>
        <button type="button" class="map-tool-btn" title="3D" @click="pitchUp">3D</button>
        <button type="button" class="map-tool-btn" title="指北" @click="resetNorth">北</button>
        <button
          type="button"
          class="map-tool-btn"
          :class="{ 'is-on': orbiting }"
          title="环绕"
          :disabled="flying"
          @click="toggleOrbit"
        >
          环绕
        </button>
      </div>
    </div>

    <div v-if="mapRevealed" class="map3d-legend">
      <span class="legend-dot" />
      <span>OpenFreeMap · 3D · 航线飞航</span>
    </div>
  </div>
</template>
