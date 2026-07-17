<script setup>
import { computed, ref, watch } from 'vue';
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

const router = useRouter();
const route = useRoute();
const mapRef = ref(null);
const selected = ref(PUBLIC_CITIES.includes(String(route.query.city)) ? String(route.query.city) : '杭州');
const publicData = ref(null);
const loading = ref(false);
const error = ref('');
const activeFilter = ref('all');
const selectedPlaceIds = ref([]);
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

function formatTime(value) {
  if (!value) return '更新时间待确认';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
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
    if (token === requestId) error.value = '公开情报暂时没有取回，仍可使用地图浏览城市；稍后切换城市可重试。';
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
  router.replace({ query: { ...route.query, city } });
  loadPublicData(city);
}, { immediate: true });
</script>

<template>
  <main class="travel-intelligence">
    <header class="intelligence-heading">
      <div>
        <p class="intelligence-kicker">行前侦察图 · 公开数据演示</p>
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
        <span>现在 · {{ selected }}</span>
        <strong>{{ Math.round(publicData.weather.temperature) }}°</strong>
        <p>{{ publicData.weather.condition }} · 风速 {{ publicData.weather.wind_speed }} km/h</p>
        <small>Open-Meteo · {{ formatTime(publicData.weather.updated_at) }}</small>
        <details v-if="publicData.weather.daily?.length">
          <summary>看未来 16 天</summary>
          <div class="forecast-row">
            <span v-for="day in publicData.weather.daily" :key="day.date">
              {{ day.date.slice(5) }}<b>{{ day.dayTemp }}° / {{ day.nightTemp }}°</b>
            </span>
          </div>
        </details>
      </aside>

      <div class="intel-rail" aria-label="当前城市旅行情报">
        <article v-if="loading" class="intel-card intel-card--status">
          <small>正在联络公开数据源</small>
          <strong>{{ selected }}情报读取中…</strong>
          <p>地图可以先看，天气和地点会陆续出现。</p>
        </article>
        <article v-else-if="error" class="intel-card intel-card--status">
          <small>底图仍可用</small>
          <strong>情报暂时没取回</strong>
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
          <small>{{ KIND_LABEL[place.kind] }} · OpenStreetMap</small>
          <strong>{{ place.name }}</strong>
          <p>{{ selectedPlaceIds.includes(place.id) ? '已带入本次规划' : '点一下定位，也可加入规划' }}</p>
        </button>

        <button v-if="showRoute" type="button" class="intel-card intel-card--route" @click="flyTo(visiblePlaces[0])">
          <small>路线估算 · OSRM</small>
          <strong>{{ publicData.route.from }} → {{ publicData.route.to }}</strong>
          <p>{{ publicData.route.distance_km.toFixed(1) }} km · 驾车约 {{ publicData.route.duration_minutes }} 分钟</p>
          <em>不代表行程顺序 · 不含实时拥堵</em>
        </button>

        <button v-if="showArrival && publicData?.airport" type="button" class="intel-card" @click="flyTo(publicData.airport)">
          <small>机场资料 · OurAirports</small>
          <strong>{{ publicData.airport.code }} · {{ publicData.airport.name }}</strong>
          <p>公开资料快照，不是实时航班接口。</p>
        </button>

        <a
          v-if="showArrival && publicData?.railway_check"
          class="intel-card intel-card--rail"
          :href="publicData.railway_check.url"
          target="_blank"
          rel="noopener noreferrer"
        >
          <small>铁路核验 · 12306 官网</small>
          <strong>去官网查车次和余票 ↗</strong>
          <p>本平台不查询、不缓存，也不展示虚构铁路信息。</p>
        </a>

        <article v-if="!loading && !error && !visiblePlaces.length && !showRoute && !showArrival" class="intel-card intel-card--status">
          <small>公开数据状态</small>
          <strong>这一层暂时没有可展示的信息</strong>
          <p>切回“全部”继续浏览，或稍后重试。</p>
        </article>
      </div>
    </section>

    <footer class="intelligence-action">
      <div>
        <span>{{ dest.tag }} · {{ selected }}</span>
        <strong v-if="selectedPlaces.length">已选 {{ selectedPlaces.length }} 个地点：{{ selectedPlaces.map((item) => item.name).join('、') }}</strong>
        <strong v-else>点地图上的公开地点，组合你想去的这一程</strong>
      </div>
      <button type="button" class="plan-cta" @click="planHere">
        {{ selectedPlaces.length ? `带着 ${selectedPlaces.length} 个地点去规划` : `规划${selected}行程` }}
      </button>
    </footer>

    <p class="data-boundary">
      OpenFreeMap / OpenStreetMap 提供底图与公开地点；天气、路线和机场资料按来源标注。这里不提供价格、库存、营业状态、实时拥堵、航班和铁路余票。
    </p>
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
  width: min(260px, calc(100% - 28px));
  padding: 16px 18px;
  border: 1px solid rgba(255, 250, 241, .66);
  border-radius: 17px;
  background: rgba(23, 63, 80, .88);
  color: var(--rice);
  box-shadow: 0 16px 32px rgba(23, 63, 80, .2);
  backdrop-filter: blur(12px);
}

.weather-ticket > span,
.weather-ticket small { display: block; color: rgba(255, 250, 241, .7); font-size: 11px; }
.weather-ticket strong { display: block; margin: 4px 0 -2px; font: 800 42px/1 var(--font-display); }
.weather-ticket p { margin: 6px 0; font-size: 13px; }
.weather-ticket details { margin-top: 9px; border-top: 1px solid rgba(255,255,255,.18); padding-top: 8px; }
.weather-ticket summary { cursor: pointer; font-size: 12px; font-weight: 700; }
.forecast-row { display: flex; gap: 10px; overflow-x: auto; margin-top: 9px; }
.forecast-row span { flex: 0 0 auto; font-size: 10px; color: rgba(255,255,255,.72); }
.forecast-row b { display: block; color: #fff; font-size: 11px; }

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
  display: block;
  flex: 0 0 clamp(220px, 25vw, 300px);
  min-height: 128px;
  padding: 15px 16px;
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
.intel-card small { display: block; color: #6c7779; font: 700 10px/1.3 var(--font-mono); }
.intel-card strong { display: block; margin: 9px 0 6px; font: 800 16px/1.3 var(--font-display); }
.intel-card p { margin: 0; color: #5d686a; font-size: 12px; line-height: 1.5; }
.intel-card em { display: block; margin-top: 6px; color: #98613e; font-size: 10px; font-style: normal; }
.intel-card--route { border-top: 4px solid var(--vermilion); }
.intel-card--rail { background: rgba(23, 63, 80, .94); color: var(--rice); }
.intel-card--rail small,
.intel-card--rail p { color: rgba(255, 250, 241, .7); }

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
a:focus-visible,
summary:focus-visible { outline: 3px solid rgba(232,112,34,.35); outline-offset: 3px; }

@media (max-width: 700px) {
  .intelligence-heading { display: grid; gap: 14px; }
  .city-tabs { width: 100%; }
  .intelligence-stage { border-radius: 18px; }
  .intelligence-map :deep(.map3d) { height: 54vh !important; min-height: 460px; border-radius: 17px; }
  .layer-strip { top: 62px; }
  .weather-ticket { top: 108px; padding: 12px 14px; width: 210px; }
  .weather-ticket strong { font-size: 34px; }
  .intel-rail { left: 10px; right: 10px; bottom: 30px; }
  .intel-card { flex-basis: min(78vw, 270px); min-height: 118px; }
  .intelligence-action { align-items: stretch; flex-direction: column; padding-top: 14px; }
  .intelligence-action strong { white-space: normal; }
  .plan-cta { width: 100%; }
}

@media (prefers-reduced-motion: reduce) {
  button.intel-card,
  a.intel-card { transition: none; }
}
</style>
