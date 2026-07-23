<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { tripApi } from '../api/trip.js';
import TravelMap3D from '../components/map/AsyncTravelMap3D.vue';
import { findDestination, geoDestinations } from '../data/geoDestinations.js';
import { normalizeRouteIntent, routeIntentFromTrack, ROUTE_INTENT_KEY } from '../map/trackEditor.js';

const CITY_NAMES = geoDestinations.map((item) => item.city);
const FILTERS = [
  ['all', '全部'], ['attraction', '景点'], ['hotel', '住宿'],
  ['restaurant', '餐饮'], ['route', '路线'], ['arrival', '抵达'],
];
const KIND_LABEL = { attraction: '景点', hotel: '住宿', restaurant: '餐饮' };
const KIND_ICON = { attraction: '🏞️', hotel: '🛏️', restaurant: '🍜' };

const router = useRouter();
const route = useRoute();
const mapRef = ref(null);
const selected = ref(CITY_NAMES.includes(String(route.query.city)) ? String(route.query.city) : '杭州');
const restoredRoute = restoreStoredRoute(selected.value);
const initialTrackPoints = ref(restoredRoute?.nodes || []);
const publicData = ref(null);
const loading = ref(false);
const error = ref('');
const activeFilter = ref('all');
const selectedPlaceIds = ref([]);
const trackIntent = ref(restoredRoute);
const weatherOpen = ref(false);
const weatherCloseRef = ref(null);
const weatherEntryRef = ref(null);
const forecastListRef = ref(null);
let requestId = 0;

function restoreStoredRoute(city) {
  try {
    return normalizeRouteIntent(JSON.parse(window.sessionStorage.getItem(ROUTE_INTENT_KEY) || 'null'), city);
  } catch {
    return null;
  }
}

const dest = computed(() => findDestination(selected.value));
const selectedPlaces = computed(() => (publicData.value?.places || [])
  .filter((place) => selectedPlaceIds.value.includes(place.id)));
const visiblePlaces = computed(() => {
  const places = publicData.value?.places || [];
  if (['attraction', 'hotel', 'restaurant'].includes(activeFilter.value)) {
    return places.filter((place) => place.kind === activeFilter.value);
  }
  if (activeFilter.value === 'route') {
    const names = [publicData.value?.route?.from, publicData.value?.route?.to];
    return places.filter((place) => names.includes(place.name));
  }
  return ['weather', 'arrival'].includes(activeFilter.value) ? [] : places;
});
const mapData = computed(() => publicData.value ? {
  ...publicData.value,
  places: visiblePlaces.value.map((place) => ({ ...place, facts: placeFacts(place) })),
  route: ['all', 'route'].includes(activeFilter.value) ? publicData.value.route : null,
  airport: ['all', 'arrival'].includes(activeFilter.value) ? publicData.value.airport : null,
} : null);
const showRoute = computed(() => ['all', 'route'].includes(activeFilter.value) && publicData.value?.route);
const showArrival = computed(() => ['all', 'arrival'].includes(activeFilter.value));
const forecastRange = computed(() => {
  const days = publicData.value?.weather?.daily || [];
  const values = days.flatMap((day) => [day.dayTemp, day.nightTemp]).filter(Number.isFinite);
  return { min: Math.min(...values), max: Math.max(...values) };
});

function normalizePlaceName(name) {
  return String(name || '').normalize('NFKC').toLocaleLowerCase('zh-CN').replace(/[\s·•・—_\-（）()]/g, '');
}

function withCuratedPlaces(data, destination) {
  const places = [...(data?.places || [])];
  const names = new Set(places.map((place) => normalizePlaceName(place.name)));
  destination.pois.forEach((poi, index) => {
    const name = normalizePlaceName(poi.name);
    if (names.has(name)) return;
    names.add(name);
    places.push({
      id: `travel-mind-${destination.city}-${index}`,
      name: poi.name,
      kind: 'attraction',
      longitude: poi.lng,
      latitude: poi.lat,
      category: '精选景点',
      source: 'Travel Mind 精选',
    });
  });
  return { ...(data || {}), city: data?.city || destination.city, places };
}

function placeFacts(place) {
  return [
    Number.isFinite(place.rating) && place.rating > 0 ? `评分 ${place.rating.toFixed(1)}` : '',
    Number.isFinite(place.cost) && place.cost > 0 ? `约 ¥${Math.round(place.cost)}` : '',
    Number.isFinite(place.distance_km) ? `距中心 ${place.distance_km.toFixed(1)} km` : '',
    place.opening_hours ? `营业 ${place.opening_hours}` : '',
    place.address || '',
  ].filter(Boolean).join(' · ');
}

function formatTime(value) {
  if (!value) return '更新时间待确认';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

function weatherGlyph(condition = '') {
  if (condition.includes('雷')) return '⚡';
  if (condition.includes('雪')) return '❄';
  if (condition.includes('雨')) return '☂';
  if (condition.includes('雾')) return '≋';
  if (condition.includes('云')) return '☁';
  return condition.includes('晴') ? '☀' : '○';
}

function formatForecastDate(value, index) {
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  const weekday = index === 0 ? '今天' : new Intl.DateTimeFormat('zh-CN', { weekday: 'short' }).format(date);
  return `${weekday} · ${date.getMonth() + 1}月${date.getDate()}日`;
}

function temperatureStyle(day) {
  const span = Math.max(1, forecastRange.value.max - forecastRange.value.min);
  const low = Math.min(day.dayTemp, day.nightTemp);
  const high = Math.max(day.dayTemp, day.nightTemp);
  return {
    left: `${((low - forecastRange.value.min) / span) * 100}%`,
    width: `${Math.max(8, ((high - low) / span) * 100)}%`,
  };
}

async function openWeather() {
  weatherOpen.value = true;
  await nextTick();
  weatherCloseRef.value?.focus();
}

function closeWeather() {
  weatherOpen.value = false;
  nextTick(() => weatherEntryRef.value?.focus());
}

function trapWeatherFocus(event) {
  const close = weatherCloseRef.value;
  const list = forecastListRef.value;
  if (!close || !list) return;
  if (event.shiftKey && document.activeElement === close) {
    event.preventDefault();
    list.focus();
  } else if (!event.shiftKey && document.activeElement === list) {
    event.preventDefault();
    close.focus();
  }
}

async function loadPublicData(city) {
  const token = ++requestId;
  loading.value = true;
  error.value = '';
  publicData.value = null;
  try {
    const destination = findDestination(city);
    const data = await tripApi.publicMap(city, destination.lng, destination.lat);
    if (token === requestId) publicData.value = withCuratedPlaces(data, destination);
  } catch {
    if (token === requestId) {
      publicData.value = withCuratedPlaces(null, findDestination(city));
      error.value = '附近内容暂时没加载出来，地图仍然可以逛；稍后切换城市再试试。';
    }
  } finally {
    if (token === requestId) loading.value = false;
  }
}

function onCityChange(city) {
  if (!CITY_NAMES.includes(city)) return;
  selected.value = city;
}

function pickCity(city) {
  selected.value = city;
  mapRef.value?.flyToCity?.(city);
}

function togglePlace(place) {
  const index = selectedPlaceIds.value.indexOf(place.id);
  selectedPlaceIds.value = index < 0
    ? [...selectedPlaceIds.value, place.id]
    : selectedPlaceIds.value.filter((id) => id !== place.id);
  mapRef.value?.flyToPoint?.(place);
}

function flyTo(item) {
  mapRef.value?.flyToPoint?.(item);
}

async function planHere() {
  if (selectedPlaces.value.length < 2) {
    error.value = '请至少选择两个地点，或直接在地图上画出路线。';
    return;
  }
  await planTrack(routeIntentFromTrack(selected.value, selectedPlaces.value.map((place) => ({
    poiId: place.id,
    name: place.name,
    kind: place.kind,
    longitude: place.longitude,
    latitude: place.latitude,
  }))));
}

function updateTrackIntent(intent) {
  trackIntent.value = normalizeRouteIntent(intent, selected.value);
}

async function planTrack(intent) {
  const normalized = normalizeRouteIntent(intent, selected.value);
  if (!normalized) return;
  try {
    window.sessionStorage.setItem(ROUTE_INTENT_KEY, JSON.stringify(normalized));
  } catch {
    error.value = '路线草稿没能保存，请检查浏览器是否禁用了会话存储。';
    return;
  }
  await router.push({ path: '/planning', query: { ...route.query, city: selected.value, route: '1' } });
}

watch(selected, (city, previousCity) => {
  activeFilter.value = 'all';
  if (previousCity) {
    selectedPlaceIds.value = [];
    trackIntent.value = null;
    initialTrackPoints.value = [];
    mapRef.value?.clearTrack?.();
  }
  weatherOpen.value = false;
  router.replace({ query: { ...route.query, city } });
  loadPublicData(city);
}, { immediate: true });
</script>

<template>
  <main class="travel-intelligence">
    <header class="intelligence-heading">
      <div>
        <p class="intelligence-kicker">城市漫游指南</p>
        <h1>{{ selected }}，先圈出想去的地方</h1>
        <p class="intelligence-lead">地图是行程起点：选地点、连路线、补节点偏好，确认后再填写出行信息。</p>
      </div>
      <label class="city-picker">
        <span>选择目的地</span>
        <select :value="selected" aria-label="选择旅行城市" @change="pickCity($event.target.value)">
          <option v-for="item in geoDestinations" :key="item.city" :value="item.city">
            {{ item.city }} · {{ item.province }}
          </option>
        </select>
      </label>
    </header>

    <section class="intelligence-workbench" :aria-label="`${selected}旅行地图与地点清单`">
      <aside class="intelligence-sidebar">
        <button
          v-if="publicData?.weather"
          ref="weatherEntryRef"
          type="button"
          class="weather-summary"
          aria-haspopup="dialog"
          @click="openWeather"
        >
          <span class="weather-glyph" aria-hidden="true">{{ weatherGlyph(publicData.weather.condition) }}</span>
          <span class="weather-now">
            <small>现在 · {{ formatTime(publicData.weather.updated_at) }}</small>
            <strong>{{ Math.round(publicData.weather.temperature) }}°</strong>
            <span>{{ publicData.weather.condition }} · 风速 {{ publicData.weather.wind_speed }} km/h</span>
          </span>
          <span class="forecast-entry">未来 {{ publicData.weather.daily?.length || 0 }} 天 <i aria-hidden="true">›</i></span>
        </button>

        <nav class="layer-strip" aria-label="筛选旅行情报">
          <button
            v-for="filter in FILTERS"
            :key="filter[0]"
            type="button"
            :class="{ 'is-active': activeFilter === filter[0] }"
            :aria-pressed="activeFilter === filter[0]"
            @click="activeFilter = filter[0]"
          >
            {{ filter[1] }}
          </button>
        </nav>

        <div class="intel-list-heading">
          <strong>{{ activeFilter === 'all' ? '值得顺路看看' : FILTERS.find((item) => item[0] === activeFilter)?.[1] }}</strong>
          <span v-if="visiblePlaces.length">{{ visiblePlaces.length }} 个地点</span>
        </div>

        <div class="intel-list" aria-label="当前城市旅行情报">
          <article v-if="loading" class="intel-card intel-card--status">
            <small>正在准备</small>
            <strong>正在找{{ selected }}的好去处…</strong>
            <p>地图可以先逛，地点马上出现。</p>
          </article>
          <article v-else-if="error" class="intel-card intel-card--status">
            <small>地图仍可浏览</small>
            <strong>附近内容暂时没加载出来</strong>
            <p>{{ error }}</p>
          </article>

          <button
            v-for="place in visiblePlaces"
            :key="place.id"
            type="button"
            class="intel-card intel-card--place"
            :class="{ 'is-selected': selectedPlaceIds.includes(place.id), 'has-community': place.community_mentions }"
            :aria-pressed="selectedPlaceIds.includes(place.id)"
            :aria-label="`${place.name}，${selectedPlaceIds.includes(place.id) ? '已加入路线' : '加入路线'}`"
            @click="togglePlace(place)"
          >
            <span class="type-mark" :class="`type-mark--${place.kind}`" aria-hidden="true">
              <img v-if="place.image_url" :src="place.image_url" alt="" loading="lazy" referrerpolicy="no-referrer" />
              <template v-else>{{ KIND_ICON[place.kind] }}</template>
            </span>
            <span class="card-copy">
              <strong>{{ place.name }}</strong>
            </span>
            <span v-if="selectedPlaceIds.includes(place.id)" class="picked-state">已选</span>
            <span v-else class="card-arrow" aria-hidden="true">＋</span>
          </button>

          <button v-if="showRoute" type="button" class="intel-card intel-card--route" @click="flyTo(visiblePlaces[0])">
            <span class="type-mark type-mark--route" aria-hidden="true">🚗</span>
            <span class="card-copy"><small>驾车参考</small><strong>{{ publicData.route.from }} → {{ publicData.route.to }}</strong><span>{{ publicData.route.distance_km.toFixed(1) }} km · 约 {{ publicData.route.duration_minutes }} 分钟</span></span>
            <span class="card-arrow" aria-hidden="true">›</span>
          </button>

          <button v-if="showArrival && publicData?.airport" type="button" class="intel-card intel-card--airport" @click="flyTo(publicData.airport)">
            <span class="type-mark type-mark--airport" aria-hidden="true">✈</span>
            <span class="card-copy"><small>乘飞机抵达</small><strong>{{ publicData.airport.code }} · {{ publicData.airport.name }}</strong></span>
            <span class="card-arrow" aria-hidden="true">›</span>
          </button>

          <a
            v-if="showArrival && publicData?.railway_check"
            class="intel-card intel-card--rail"
            :href="publicData.railway_check.url"
            target="_blank"
            rel="noopener noreferrer"
          >
            <span class="type-mark type-mark--rail" aria-hidden="true">🚄</span>
            <span class="card-copy"><small>乘火车抵达</small><strong>去 12306 查车次与余票</strong></span>
            <span class="card-arrow" aria-hidden="true">↗</span>
          </a>

          <article v-if="!loading && !error && !visiblePlaces.length && !showRoute && !showArrival" class="intel-card intel-card--status">
            <small>暂时没有找到相关地点</small>
            <strong>换个分类看看</strong>
            <p>切回“全部”继续逛。</p>
          </article>
        </div>

        <footer class="intelligence-action">
          <div>
            <span>{{ dest.tag }} · {{ selected }}</span>
            <strong v-if="selectedPlaces.length">已选 {{ selectedPlaces.length }} 处：{{ selectedPlaces.map((item) => item.name).join('、') }}</strong>
            <strong v-else>至少选择 2 个地点，组成路线</strong>
          </div>
          <button type="button" class="plan-cta" :disabled="selectedPlaces.length < 2" @click="planHere">
            {{ selectedPlaces.length >= 2 ? `确认这 ${selectedPlaces.length} 个地点` : `还差 ${2 - selectedPlaces.length} 个地点` }}
          </button>
        </footer>
      </aside>

      <section class="intelligence-map-panel" aria-label="3D 旅行情报地图">
        <div class="map-context"><strong>{{ selected }}</strong><span>{{ trackIntent ? `路线已连成 ${trackIntent.nodes.length} 个节点` : '拖动浏览 · 开启轨迹编辑后点击落点' }}</span></div>
        <TravelMap3D
          ref="mapRef"
          class="intelligence-map"
          :city="selected"
          height="680px"
          :auto-orbit="false"
          :show-pois="false"
          :public-data="mapData"
          :selected-place-ids="selectedPlaceIds"
          :selectable-cities="CITY_NAMES"
          :initial-track-points="initialTrackPoints"
          track-editor
          @city-change="onCityChange"
          @point-select="togglePlace"
          @track-change="updateTrackIntent"
          @track-plan="planTrack"
        />
      </section>
    </section>

    <p class="data-boundary">天气来自 Open-Meteo；地点来自高德地图、OpenStreetMap 与 Travel Mind 地点库，并结合公开社区攻略。营业、价格、航班、车次与余票请以现场及官网为准。</p>

    <Teleport to="body">
      <Transition name="weather-sheet">
        <div v-if="weatherOpen" class="weather-backdrop" role="presentation" @click.self="closeWeather" @keydown.esc="closeWeather" @keydown.tab="trapWeatherFocus">
          <section class="weather-drawer" role="dialog" aria-modal="true" aria-labelledby="forecast-title">
            <header>
              <div>
                <span>{{ selected }}</span>
                <h2 id="forecast-title">未来 {{ publicData?.weather?.daily?.length || 0 }} 天</h2>
              </div>
              <button ref="weatherCloseRef" type="button" aria-label="关闭未来天气" @click="closeWeather">×</button>
            </header>
            <p class="forecast-summary">{{ publicData?.weather?.condition }}，现在 {{ Math.round(publicData?.weather?.temperature || 0) }}°</p>
            <div ref="forecastListRef" class="forecast-list" tabindex="0" aria-label="未来天气逐日预报">
              <article v-for="(day, index) in publicData?.weather?.daily || []" :key="day.date" class="forecast-day">
                <time :datetime="day.date">{{ formatForecastDate(day.date, index) }}</time>
                <span class="forecast-condition"><i aria-hidden="true">{{ weatherGlyph(day.dayWeather) }}</i>{{ day.dayWeather }}</span>
                <strong class="temp-low">{{ Math.min(day.dayTemp, day.nightTemp) }}°</strong>
                <span class="temperature-track" aria-hidden="true"><i :style="temperatureStyle(day)" /></span>
                <strong>{{ Math.max(day.dayTemp, day.nightTemp) }}°</strong>
              </article>
            </div>
          </section>
        </div>
      </Transition>
    </Teleport>
  </main>
</template>

<style scoped>
.travel-intelligence {
  --night-ink: #173f50;
  --rice: #fffaf1;
  --vermilion: #e87022;
  --map-workbench-h: clamp(680px, calc(100vh - 180px), 820px);
  width: min(1520px, calc(100vw - 40px));
  max-width: none;
  margin-left: 50%;
  transform: translateX(-50%);
}

.intelligence-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  margin-bottom: 22px;
}

.intelligence-kicker {
  margin: 0 0 8px;
  color: var(--vermilion);
  font: 800 12px/1.2 var(--font-mono);
  letter-spacing: .14em;
}

.intelligence-heading h1 {
  margin: 0;
  color: var(--night-ink);
  font: 800 clamp(30px, 4vw, 48px)/1.08 var(--font-display);
  letter-spacing: -.045em;
}

.intelligence-lead {
  margin: 9px 0 0;
  color: #6f6a63;
  font-size: 14px;
}

.city-picker {
  display: grid;
  gap: 5px;
  min-width: 230px;
}

.city-picker span {
  color: #81786e;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: .08em;
}

.city-picker select {
  width: 100%;
  min-height: 44px;
  padding: 0 40px 0 13px;
  border: 1px solid #d9d1c6;
  border-radius: 12px;
  background: #fff;
  color: var(--night-ink);
  cursor: pointer;
  font-weight: 800;
}

.intelligence-workbench {
  display: grid;
  grid-template-columns: clamp(300px, 21vw, 340px) minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid #d8d0c4;
  border-radius: 24px;
  background: var(--rice);
  box-shadow: 0 22px 54px rgba(23, 63, 80, .14);
}

.intelligence-sidebar {
  display: flex;
  height: var(--map-workbench-h);
  min-width: 0;
  flex-direction: column;
  padding: 18px;
  border-right: 1px solid #e5ddd2;
  background: #fffdf8;
}

.weather-summary {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 74px;
  padding: 0 0 14px;
  border: 0;
  border-bottom: 1px solid #e8e1d7;
  border-radius: 0;
  background: transparent;
  color: var(--night-ink);
  cursor: pointer;
  text-align: left;
}

.weather-glyph {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 10px;
  background: #fff0e4;
  color: var(--vermilion);
  font-size: 23px;
}

.weather-now { display: block; min-width: 0; }
.weather-now small { display: block; overflow: hidden; color: #8c8379; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.weather-now strong { display: inline-block; margin: 4px 8px 0 0; font: 800 25px/1 var(--font-display); vertical-align: middle; }
.weather-now > span { color: #6d7777; font-size: 11px; vertical-align: middle; white-space: nowrap; }
.forecast-entry {
  color: var(--vermilion);
  font-size: 10px;
  font-weight: 800;
  white-space: nowrap;
}
.forecast-entry i { margin-left: 3px; color: currentColor; font-size: 18px; font-style: normal; vertical-align: -1px; }

.layer-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin: 16px 0 18px;
}

.layer-strip button {
  min-height: 34px;
  padding: 0 8px;
  border: 1px solid #e4ddd4;
  border-radius: 9px;
  background: #fff;
  color: #596466;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
}

.layer-strip button.is-active {
  border-color: var(--vermilion);
  background: #fff0e4;
  color: #bd5518;
}

.intel-list-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 9px;
  color: var(--night-ink);
}

.intel-list-heading strong { flex: 0 0 auto; font-size: 13px; white-space: nowrap; }
.intel-list-heading span { color: #91887e; font-size: 11px; line-height: 1.45; text-align: right; }

.intel-list {
  display: grid;
  min-height: 0;
  overflow-y: auto;
  align-content: start;
  gap: 8px;
  padding-right: 5px;
  scrollbar-color: #d9cfc3 transparent;
}

.intel-card {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 68px;
  padding: 10px;
  border: 1px solid #e8e1d7;
  border-radius: 12px;
  background: #fff;
  color: var(--night-ink);
  text-align: left;
}

button.intel-card,
a.intel-card { cursor: pointer; transition: transform .16s ease, border-color .16s ease; }
button.intel-card:hover,
a.intel-card:hover { transform: translateX(2px); border-color: var(--vermilion); }
.intel-card.is-selected { border-color: var(--vermilion); background: #fff7ef; }
.intel-card.has-community { min-height: 68px; }
.intel-card small { display: block; margin-bottom: 3px; color: #7b817e; font: 700 10px/1.3 var(--font-body); }
.intel-card strong { display: block; overflow: hidden; font: 800 14px/1.3 var(--font-display); text-overflow: ellipsis; white-space: nowrap; }
.intel-card p { margin: 0; color: #5d686a; font-size: 12px; line-height: 1.5; }
.card-copy { display: block; min-width: 0; }
.card-copy > span { display: block; overflow: hidden; margin-top: 4px; color: #5d686a; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.card-copy .community-tip { color: #bd5518; }
.type-mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 12px 12px 12px 4px;
  background: #e9f4ef;
  font-size: 18px;
  box-shadow: inset 0 0 0 1px rgba(23,63,80,.05);
  overflow: hidden;
}
.type-mark img { width: 100%; height: 100%; object-fit: cover; }
.type-mark--hotel { border-radius: 7px 15px 7px 15px; background: #e3f1f5; }
.type-mark--restaurant { border-radius: 50% 50% 12px 50%; background: #fff0df; }
.type-mark--route { border-radius: 12px 4px 12px 4px; background: #ffe5d4; }
.type-mark--airport { border-radius: 50% 8px 50% 8px; background: #e5ebf7; color: #254a81; }
.type-mark--rail { border-radius: 8px; background: rgba(255,255,255,.14); }
.card-arrow { color: #8b9290; font-size: 20px; font-weight: 500; }
.picked-state { padding: 3px 7px; border-radius: 999px; background: var(--vermilion); color: #fff; font-size: 9px; font-weight: 800; white-space: nowrap; }
.intel-card--rail { background: rgba(23, 63, 80, .94); color: var(--rice); }
.intel-card--rail small,
.intel-card--rail .card-arrow { color: rgba(255, 250, 241, .7); }
.intel-card--status { display: block; }

.intelligence-action {
  display: grid;
  gap: 10px;
  margin: 14px -2px -2px;
  padding: 14px 2px 2px;
  border-top: 1px solid #e8e1d7;
}

.intelligence-action div { display: grid; min-width: 0; }
.intelligence-action span { color: var(--vermilion); font-size: 10px; font-weight: 800; }
.intelligence-action strong { overflow: hidden; margin-top: 2px; color: var(--night-ink); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.plan-cta { width: 100%; min-height: 44px; padding: 0 18px; border: 0; border-radius: 12px; background: var(--vermilion); color: #fff; cursor: pointer; font-weight: 800; box-shadow: 0 8px 18px rgba(232,112,34,.18); }
.plan-cta:disabled { background: #eadfd3; color: #8a8178; box-shadow: none; }

.intelligence-map-panel { position: relative; min-width: 0; overflow: hidden; background: #e9e4db; }
.intelligence-map :deep(.map3d) { height: var(--map-workbench-h) !important; border-radius: 0 23px 23px 0; }
.intelligence-map :deep(.map3d-city-picker) { display: none; }
.intelligence-map :deep(.map3d-hud) { justify-content: flex-end; }
.intelligence-map :deep(.map3d-tools) { gap: 4px; }
.intelligence-map :deep(.map-tool-btn) { min-width: 38px; height: 38px; font-size: 12px; }
.map-context {
  position: absolute;
  z-index: 9;
  top: 16px;
  left: 16px;
  display: grid;
  padding: 10px 13px;
  border: 1px solid rgba(255,255,255,.72);
  border-radius: 12px;
  background: rgba(255,253,248,.9);
  color: var(--night-ink);
  box-shadow: 0 7px 20px rgba(23,63,80,.12);
  backdrop-filter: blur(10px);
  pointer-events: none;
}
.map-context strong { font-size: 13px; }
.map-context span { color: #6d7777; font-size: 10px; }

.weather-backdrop {
  position: fixed;
  z-index: 1000;
  inset: 0;
  display: grid;
  align-items: center;
  justify-items: end;
  padding: 20px;
  background: rgba(39, 35, 31, .32);
  backdrop-filter: blur(4px);
}
.weather-drawer {
  --night-ink: #173f50;
  --rice: #fffaf1;
  --vermilion: #e87022;
  display: flex;
  flex-direction: column;
  width: min(500px, 100%);
  max-height: min(790px, calc(100vh - 40px));
  overflow: hidden;
  border: 1px solid #d8d0c4;
  border-radius: 22px;
  background: #fffdf8;
  color: var(--night-ink);
  box-shadow: 0 28px 70px rgba(39,35,31,.24);
}
.weather-drawer header { display: flex; justify-content: space-between; align-items: start; padding: 24px 24px 10px; background: var(--night-ink); color: var(--rice); }
.weather-drawer header span { color: rgba(255,250,241,.64); font-size: 11px; font-weight: 700; letter-spacing: .08em; }
.weather-drawer h2 { margin: 3px 0 0; font: 800 27px/1.15 var(--font-display); }
.weather-drawer header button { display: grid; width: 34px; height: 34px; padding: 0; place-items: center; border: 1px solid rgba(255,250,241,.24); border-radius: 10px; background: transparent; color: var(--rice); cursor: pointer; font-size: 22px; line-height: 1; }
.forecast-summary { margin: 0; padding: 0 24px 20px; background: var(--night-ink); color: rgba(255,250,241,.76); font-size: 13px; }
.forecast-list { overflow-y: auto; padding: 6px 18px 18px; scrollbar-color: #d4c9bc transparent; }
.forecast-day {
  display: grid;
  grid-template-columns: 110px 82px 30px minmax(70px, 1fr) 30px;
  align-items: center;
  gap: 10px;
  min-height: 56px;
  padding: 0 8px;
  border-bottom: 1px solid #ece5dc;
}
.forecast-day:last-child { border-bottom: 0; }
.forecast-day time { color: var(--night-ink); font-size: 12px; font-weight: 800; }
.forecast-condition { display: flex; align-items: center; gap: 7px; color: #6d7777; font-size: 11px; }
.forecast-condition i { width: 24px; color: var(--vermilion); font-size: 20px; font-style: normal; text-align: center; }
.forecast-day strong { font-size: 14px; text-align: right; }
.forecast-day .temp-low { color: #8b9290; }
.temperature-track { position: relative; height: 4px; border-radius: 999px; background: #e4ddd4; }
.temperature-track i { position: absolute; top: 0; bottom: 0; min-width: 8px; border-radius: inherit; background: var(--vermilion); }
.weather-sheet-enter-active .weather-drawer,
.weather-sheet-leave-active .weather-drawer { transition: transform .22s ease; }
.weather-sheet-enter-from .weather-drawer,
.weather-sheet-leave-to .weather-drawer { transform: translateX(28px); }

.data-boundary { margin: 12px 4px 0; color: #7d756c; font-size: 11px; line-height: 1.6; }

button:focus-visible,
a:focus-visible { outline: 3px solid rgba(232,112,34,.35); outline-offset: 3px; }
.weather-summary:focus-visible { outline: 3px solid rgba(232,112,34,.45); outline-offset: 3px; }

@media (max-width: 820px) {
  .travel-intelligence { width: 100%; margin-left: 0; transform: none; }
  .intelligence-heading { display: grid; gap: 14px; }
  .city-picker { width: 100%; }
  .intelligence-workbench { grid-template-columns: 1fr; border-radius: 18px; }
  .intelligence-sidebar { height: auto; min-height: 540px; border-right: 0; border-bottom: 1px solid #e5ddd2; }
  .intel-list { max-height: 360px; }
  .intelligence-map :deep(.map3d) { height: 56vh !important; min-height: 440px; border-radius: 0 0 17px 17px; }
  .weather-backdrop { align-items: end; padding: 0; }
  .weather-drawer { width: 100%; max-height: 82vh; border-radius: 20px 20px 0 0; }
  .weather-drawer header { padding: 22px 20px 10px; }
  .forecast-summary { padding: 0 20px 18px; }
  .forecast-list { padding: 6px 10px 18px; }
  .forecast-day { grid-template-columns: 90px 48px 28px minmax(50px, 1fr) 28px; gap: 7px; padding: 0 8px; }
  .forecast-condition { justify-content: center; }
  .forecast-condition i { font-size: 20px; }
  .forecast-condition { font-size: 0; }
  .weather-sheet-enter-from .weather-drawer,
  .weather-sheet-leave-to .weather-drawer { transform: translateY(28px); }
}

@media (max-width: 480px) {
  .intelligence-heading h1 { font-size: 30px; }
  .intelligence-lead { font-size: 13px; }
  .intelligence-sidebar { padding: 14px; }
  .weather-summary { grid-template-columns: 34px minmax(0, 1fr); }
  .forecast-entry { grid-column: 2; margin-top: -6px; }
  .layer-strip { margin: 14px 0 16px; }
  .map-context { top: 12px; left: 12px; }
}

@media (prefers-reduced-motion: reduce) {
  button.intel-card,
  a.intel-card,
  .weather-sheet-enter-active .weather-drawer,
  .weather-sheet-leave-active .weather-drawer { transition: none; }
}
</style>
