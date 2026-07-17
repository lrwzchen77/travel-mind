<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { tripApi } from '../api/trip.js';
import TravelMap3D from '../components/map/AsyncTravelMap3D.vue';
import { findDestination } from '../data/geoDestinations.js';

const PUBLIC_CITIES = ['杭州', '北京', '成都'];
const FILTERS = [
  ['all', '全部'], ['weather', '天气'], ['attraction', '景点'], ['hotel', '住宿'],
  ['restaurant', '餐饮'], ['route', '路线'], ['arrival', '怎么抵达'],
];
const KIND_LABEL = { attraction: '景点', hotel: '住宿', restaurant: '餐饮' };
const KIND_ICON = { attraction: '🏞️', hotel: '🛏️', restaurant: '🍜' };

const router = useRouter();
const route = useRoute();
const mapRef = ref(null);
const selected = ref(PUBLIC_CITIES.includes(String(route.query.city)) ? String(route.query.city) : '杭州');
const publicData = ref(null);
const loading = ref(false);
const error = ref('');
const activeFilter = ref('all');
const selectedPlaceIds = ref([]);
const weatherOpen = ref(false);
const weatherCloseRef = ref(null);
const weatherEntryRef = ref(null);
const forecastListRef = ref(null);
let requestId = 0;

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
  places: visiblePlaces.value,
  route: ['all', 'route'].includes(activeFilter.value) ? publicData.value.route : null,
  airport: ['all', 'arrival'].includes(activeFilter.value) ? publicData.value.airport : null,
} : null);
const showWeather = computed(() => ['all', 'weather'].includes(activeFilter.value));
const showRoute = computed(() => ['all', 'route'].includes(activeFilter.value) && publicData.value?.route);
const showArrival = computed(() => ['all', 'arrival'].includes(activeFilter.value));
const forecastRange = computed(() => {
  const days = publicData.value?.weather?.daily || [];
  const values = days.flatMap((day) => [day.dayTemp, day.nightTemp]).filter(Number.isFinite);
  return { min: Math.min(...values), max: Math.max(...values) };
});

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
    const data = await tripApi.publicMap(city);
    if (token === requestId) publicData.value = data;
  } catch {
    if (token === requestId) error.value = '附近内容暂时没加载出来，地图仍然可以逛；稍后切换城市再试试。';
  } finally {
    if (token === requestId) loading.value = false;
  }
}

function onCityChange(city) {
  if (!PUBLIC_CITIES.includes(city)) return;
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

function planHere() {
  const names = selectedPlaces.value.map((place) => place.name).join('、');
  router.push({
    path: '/planning',
    query: { city: selected.value, ...(names ? { poi: names } : {}) },
  });
}

watch(selected, (city) => {
  activeFilter.value = 'all';
  selectedPlaceIds.value = [];
  weatherOpen.value = false;
  router.replace({ query: { ...route.query, city } });
  loadPublicData(city);
}, { immediate: true });
</script>

<template>
  <main class="travel-intelligence">
    <header class="intelligence-heading">
      <div>
        <p class="intelligence-kicker">{{ selected }}旅行地图</p>
        <h1>先看清一座城，再决定怎么玩</h1>
      </div>
      <div class="city-tabs" aria-label="选择城市">
        <button
          v-for="city in PUBLIC_CITIES"
          :key="city"
          type="button"
          :class="{ 'is-active': selected === city }"
          :aria-pressed="selected === city"
          @click="pickCity(city)"
        >
          {{ city }}
        </button>
      </div>
    </header>

    <section class="intelligence-stage" aria-label="3D 旅行情报地图">
      <TravelMap3D
        ref="mapRef"
        class="intelligence-map"
        :city="selected"
        height="min(76vh, 760px)"
        :auto-orbit="false"
        :show-pois="false"
        :public-data="mapData"
        :selected-place-ids="selectedPlaceIds"
        :selectable-cities="PUBLIC_CITIES"
        @city-change="onCityChange"
        @point-select="togglePlace"
      />

      <nav class="layer-strip" aria-label="地图情报筛选">
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

      <aside v-if="publicData?.weather && showWeather" class="weather-ticket" aria-live="polite">
        <span class="weather-glyph" aria-hidden="true">{{ weatherGlyph(publicData.weather.condition) }}</span>
        <div>
          <span>现在 · {{ selected }}</span>
          <strong>{{ Math.round(publicData.weather.temperature) }}°</strong>
          <p>{{ publicData.weather.condition }} · 风速 {{ publicData.weather.wind_speed }} km/h</p>
          <small>{{ formatTime(publicData.weather.updated_at) }} 更新</small>
        </div>
        <button v-if="publicData.weather.daily?.length" ref="weatherEntryRef" type="button" class="forecast-entry" @click="openWeather">
          未来 {{ publicData.weather.daily.length }} 天 <span aria-hidden="true">›</span>
        </button>
      </aside>

      <div class="intel-rail" aria-label="当前城市旅行情报">
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
          :class="{ 'is-selected': selectedPlaceIds.includes(place.id) }"
          :aria-pressed="selectedPlaceIds.includes(place.id)"
          @click="togglePlace(place)"
        >
          <span class="type-mark" :class="`type-mark--${place.kind}`" aria-hidden="true">{{ KIND_ICON[place.kind] }}</span>
          <span class="card-copy"><small>{{ KIND_LABEL[place.kind] }}</small><strong>{{ place.name }}</strong></span>
          <span v-if="selectedPlaceIds.includes(place.id)" class="picked-state">✓ 已加入</span>
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
    </section>

    <footer class="intelligence-action">
      <div>
        <span>{{ dest.tag }} · {{ selected }}</span>
        <strong v-if="selectedPlaces.length">已选 {{ selectedPlaces.length }} 个地点：{{ selectedPlaces.map((item) => item.name).join('、') }}</strong>
        <strong v-else>点地图上的好去处，组合你想去的这一程</strong>
      </div>
      <button type="button" class="plan-cta" @click="planHere">
        {{ selectedPlaces.length ? `带着 ${selectedPlaces.length} 个地点去规划` : `规划${selected}行程` }}
      </button>
    </footer>

    <p class="data-boundary">天气来自 Open-Meteo；地点与路线来自开放地图资料，仅作行前参考。航班、车次与余票请以官网为准。</p>

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
  width: min(1320px, 100%);
  margin: 0 auto;
}

.intelligence-heading {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 24px;
  margin-bottom: 18px;
}

.intelligence-kicker {
  margin: 0 0 7px;
  color: var(--vermilion);
  font: 700 11px/1.2 var(--font-mono);
  letter-spacing: .08em;
}

.intelligence-heading h1 {
  max-width: none;
  margin: 0;
  color: var(--night-ink);
  font: 800 clamp(28px, 4vw, 46px)/1.08 var(--font-display);
  letter-spacing: -.035em;
}

.city-tabs,
.layer-strip {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  scrollbar-width: none;
}

.city-tabs button,
.layer-strip button {
  flex: 0 0 auto;
  border: 1px solid rgba(255, 250, 241, .65);
  border-radius: 999px;
  background: rgba(255, 250, 241, .88);
  color: var(--night-ink);
  cursor: pointer;
  font-weight: 700;
}

.city-tabs button {
  min-height: 38px;
  padding: 0 17px;
  border-color: #d9d1c6;
}

.city-tabs button.is-active,
.layer-strip button.is-active {
  border-color: var(--vermilion);
  background: var(--vermilion);
  color: #fff;
}

.intelligence-stage {
  position: relative;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #d8d0c4;
  border-radius: 24px;
  background: var(--night-ink);
  box-shadow: 0 24px 60px rgba(23, 63, 80, .18);
}

.intelligence-map :deep(.map3d) { border-radius: 23px; }

.layer-strip {
  position: absolute;
  z-index: 8;
  top: 66px;
  left: 12px;
  right: 12px;
  padding-bottom: 4px;
  pointer-events: auto;
}

.layer-strip button {
  min-height: 34px;
  padding: 0 13px;
  box-shadow: 0 5px 18px rgba(23, 63, 80, .14);
  backdrop-filter: blur(10px);
  font-size: 12px;
}

.weather-ticket {
  position: absolute;
  z-index: 7;
  top: 112px;
  left: 14px;
  display: grid;
  grid-template-columns: 48px 1fr;
  width: min(270px, calc(100% - 28px));
  padding: 14px 15px 12px;
  border: 1px solid rgba(255, 250, 241, .66);
  border-radius: 20px;
  background: linear-gradient(145deg, rgba(43, 103, 132, .94), rgba(23, 63, 80, .92));
  color: var(--rice);
  box-shadow: 0 16px 32px rgba(23, 63, 80, .2);
  backdrop-filter: blur(12px);
}

.weather-glyph { padding-top: 5px; color: #ffd65a; font-size: 35px; line-height: 1; text-align: center; text-shadow: 0 3px 15px rgba(255,214,90,.3); }
.weather-ticket > div > span,
.weather-ticket small { display: block; color: rgba(255, 250, 241, .72); font-size: 11px; }
.weather-ticket strong { display: block; margin: 2px 0 -1px; font: 800 38px/1 var(--font-display); }
.weather-ticket p { margin: 5px 0; font-size: 12px; }
.forecast-entry {
  grid-column: 1 / -1;
  display: flex;
  justify-content: space-between;
  width: 100%;
  margin-top: 11px;
  padding: 10px 2px 0;
  border: 0;
  border-top: 1px solid rgba(255,255,255,.18);
  background: transparent;
  color: #fff;
  cursor: pointer;
  font-weight: 800;
  text-align: left;
}
.forecast-entry span { font-size: 20px; line-height: .7; }

.intel-rail {
  position: absolute;
  z-index: 8;
  left: 14px;
  right: 14px;
  bottom: 35px;
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 6px 2px 8px;
  scroll-snap-type: x proximity;
  scrollbar-width: thin;
}

.intel-card {
  position: relative;
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) auto;
  align-items: center;
  gap: 11px;
  flex: 0 0 clamp(220px, 25vw, 300px);
  min-height: 104px;
  padding: 14px;
  border: 1px solid rgba(255, 250, 241, .72);
  border-radius: 16px;
  background: rgba(255, 250, 241, .94);
  color: var(--night-ink);
  box-shadow: 0 12px 30px rgba(23, 63, 80, .2);
  scroll-snap-align: start;
  text-align: left;
  backdrop-filter: blur(12px);
}

button.intel-card,
a.intel-card { cursor: pointer; transition: transform .16s ease, border-color .16s ease; }
button.intel-card:hover,
a.intel-card:hover { transform: translateY(-3px); border-color: var(--vermilion); }
.intel-card.is-selected { border: 2px solid var(--vermilion); background: #fff; }
.intel-card small { display: block; margin-bottom: 5px; color: #6c7779; font: 700 11px/1.3 var(--font-body); }
.intel-card strong { display: block; overflow: hidden; font: 800 16px/1.3 var(--font-display); text-overflow: ellipsis; }
.intel-card p { margin: 0; color: #5d686a; font-size: 12px; line-height: 1.5; }
.card-copy { display: block; min-width: 0; }
.card-copy > span { display: block; margin-top: 6px; color: #5d686a; font-size: 12px; }
.type-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 15px 15px 15px 5px;
  background: #e9f4ef;
  font-size: 22px;
  box-shadow: inset 0 0 0 1px rgba(23,63,80,.05);
}
.type-mark--hotel { border-radius: 8px 17px 8px 17px; background: #e3f1f5; }
.type-mark--restaurant { border-radius: 50% 50% 12px 50%; background: #fff0df; }
.type-mark--route { border-radius: 14px 5px 14px 5px; background: #ffe5d4; }
.type-mark--airport { border-radius: 50% 8px 50% 8px; background: #e5ebf7; color: #254a81; }
.type-mark--rail { border-radius: 8px; background: rgba(255,255,255,.14); }
.card-arrow { color: #7b8789; font-size: 24px; font-weight: 500; }
.picked-state { align-self: start; padding: 4px 7px; border-radius: 999px; background: var(--vermilion); color: #fff; font-size: 10px; font-weight: 800; white-space: nowrap; }
.intel-card--rail { background: rgba(23, 63, 80, .94); color: var(--rice); }
.intel-card--rail small,
.intel-card--rail .card-arrow { color: rgba(255, 250, 241, .7); }
.intel-card--status { display: block; }

.weather-backdrop {
  position: fixed;
  z-index: 1000;
  inset: 0;
  display: grid;
  align-items: center;
  justify-items: end;
  padding: 24px;
  background: rgba(12, 32, 43, .45);
  backdrop-filter: blur(7px);
}
.weather-drawer {
  display: flex;
  flex-direction: column;
  width: min(520px, 100%);
  max-height: min(820px, calc(100vh - 48px));
  overflow: hidden;
  border: 1px solid rgba(255,255,255,.35);
  border-radius: 30px;
  background: linear-gradient(165deg, #377da0 0%, #286782 42%, #194659 100%);
  color: #fff;
  box-shadow: 0 34px 90px rgba(12,32,43,.38);
}
.weather-drawer header { display: flex; justify-content: space-between; align-items: start; padding: 28px 28px 12px; }
.weather-drawer header span { color: rgba(255,255,255,.7); font-size: 13px; font-weight: 700; }
.weather-drawer h2 { margin: 2px 0 0; font: 800 30px/1.15 var(--font-display); }
.weather-drawer header button { display: grid; width: 36px; height: 36px; padding: 0; place-items: center; border: 0; border-radius: 50%; background: rgba(8,40,53,.35); color: #fff; cursor: pointer; font-size: 25px; line-height: 1; }
.forecast-summary { margin: 0 28px 16px; color: rgba(255,255,255,.78); font-size: 14px; }
.forecast-list { overflow-y: auto; padding: 0 18px 20px; scrollbar-color: rgba(255,255,255,.32) transparent; }
.forecast-day {
  display: grid;
  grid-template-columns: 112px 88px 32px minmax(70px, 1fr) 32px;
  align-items: center;
  gap: 10px;
  min-height: 58px;
  padding: 0 10px;
  border-top: 1px solid rgba(255,255,255,.16);
}
.forecast-day time { font-size: 13px; font-weight: 750; }
.forecast-condition { display: flex; align-items: center; gap: 7px; color: rgba(255,255,255,.8); font-size: 12px; }
.forecast-condition i { width: 24px; color: #ffd65a; font-size: 22px; font-style: normal; text-align: center; }
.forecast-day strong { font-size: 14px; text-align: right; }
.forecast-day .temp-low { color: rgba(255,255,255,.63); }
.temperature-track { position: relative; height: 5px; border-radius: 999px; background: rgba(255,255,255,.18); }
.temperature-track i { position: absolute; top: 0; bottom: 0; min-width: 8px; border-radius: inherit; background: linear-gradient(90deg, #70d8ee, #ffd65a, #ff9c4b); }
.weather-sheet-enter-active .weather-drawer,
.weather-sheet-leave-active .weather-drawer { transition: transform .22s ease; }
.weather-sheet-enter-from .weather-drawer,
.weather-sheet-leave-to .weather-drawer { transform: translateX(28px); }

.intelligence-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 18px 4px 4px;
}

.intelligence-action div { display: grid; min-width: 0; }
.intelligence-action span { color: var(--vermilion); font-size: 12px; font-weight: 800; }
.intelligence-action strong { overflow: hidden; color: var(--night-ink); font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
.plan-cta { min-height: 46px; padding: 0 22px; border: 0; border-radius: 999px; background: var(--vermilion); color: #fff; cursor: pointer; font-weight: 800; box-shadow: 0 10px 24px rgba(232,112,34,.2); }
.data-boundary { margin: 12px 4px 0; color: #7d756c; font-size: 11px; line-height: 1.6; }

button:focus-visible,
a:focus-visible { outline: 3px solid rgba(232,112,34,.35); outline-offset: 3px; }
.forecast-entry:focus-visible { outline: 3px solid rgba(255,255,255,.45); outline-offset: 4px; }

@media (max-width: 700px) {
  .intelligence-heading { display: grid; gap: 14px; }
  .city-tabs { width: 100%; }
  .intelligence-stage { border-radius: 18px; }
  .intelligence-map :deep(.map3d) { height: 54vh !important; min-height: 460px; border-radius: 17px; }
  .layer-strip { top: 62px; }
  .weather-ticket { top: 108px; padding: 12px; width: 224px; }
  .weather-ticket strong { font-size: 34px; }
  .intel-rail { left: 10px; right: 10px; bottom: 30px; }
  .intel-card { flex-basis: min(78vw, 270px); min-height: 118px; }
  .intelligence-action { align-items: stretch; flex-direction: column; padding-top: 14px; }
  .intelligence-action strong { white-space: normal; }
  .plan-cta { width: 100%; }
  .weather-backdrop { align-items: end; padding: 0; }
  .weather-drawer { width: 100%; max-height: 82vh; border-radius: 26px 26px 0 0; }
  .weather-drawer header { padding: 22px 20px 10px; }
  .forecast-summary { margin: 0 20px 12px; }
  .forecast-list { padding: 0 10px 18px; }
  .forecast-day { grid-template-columns: 90px 48px 28px minmax(50px, 1fr) 28px; gap: 7px; padding: 0 8px; }
  .forecast-condition { justify-content: center; }
  .forecast-condition i { font-size: 20px; }
  .forecast-condition { font-size: 0; }
  .weather-sheet-enter-from .weather-drawer,
  .weather-sheet-leave-to .weather-drawer { transform: translateY(28px); }
}

@media (prefers-reduced-motion: reduce) {
  button.intel-card,
  a.intel-card,
  .weather-sheet-enter-active .weather-drawer,
  .weather-sheet-leave-active .weather-drawer { transition: none; }
}
</style>
