<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { resourceApi } from '../api/resources.js';
import { cityImageByName } from '../data/cityImages.js';
import { findDestination } from '../data/geoDestinations.js';
import { cityVisionInsight } from '../data/visionInsights.js';
import { useFavorites } from '../composables/useFavorites.js';

const route = useRoute();
const router = useRouter();
const cityName = computed(() => String(route.params.city || '').trim());
const city = ref(null);
const loading = ref(true);
const error = ref('');
const destination = computed(() => findDestination(cityName.value));
const cover = computed(() => cityImageByName[cityName.value] || '');
const insight = computed(() => cityVisionInsight(cityName.value));
const { busyKey, isFavorite, loadFavorites, toggleFavorite } = useFavorites();

const preferences = computed(() => {
  const text = `${destination.value.tag} ${destination.value.blurb}`;
  const values = [];
  if (/[江河湖海湾岛泉]/.test(text)) values.push('湖景');
  if (/[味食火锅早茶面夜市]/.test(text)) values.push('美食');
  if (/[夜霓虹灯火]/.test(text)) values.push('夜景');
  if (/[古博物文脉遗址故宫]/.test(text)) values.push('博物馆');
  if (/[山草原雪徒步]/.test(text)) values.push('徒步');
  if (!values.length) values.push('轻松', '拍照');
  return [...new Set(values)].slice(0, 4);
});

const planningLink = computed(() => ({
  path: '/planning',
  query: { city: cityName.value, preferences: preferences.value.join(',') },
}));

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const data = await resourceApi.discover('cities', { keyword: cityName.value, pageSize: 50 });
    city.value = (data.records || []).find((item) => item.name === cityName.value) || null;
    try {
      await loadFavorites();
    } catch {
      // City information should not depend on the personal favorites service.
    }
  } catch (err) {
    error.value = err?.message || '城市资料暂时没有加载完整';
  } finally {
    loading.value = false;
  }
}

async function favorite() {
  if (!city.value?.id) return;
  error.value = '';
  try {
    const result = await toggleFavorite({
      targetType: 'city',
      targetId: city.value.id,
      note: cityName.value,
    });
    if (result.requiresLogin) {
      router.push({ path: '/login', query: { redirect: route.fullPath } });
    }
  } catch (err) {
    error.value = err?.message || '收藏操作没有成功';
  }
}

function poiPlanningLink(poi) {
  return {
    path: '/planning',
    query: { city: cityName.value, poi: poi.name, preferences: preferences.value.join(',') },
  };
}

onMounted(load);
</script>

<template>
  <RouterLink class="city-detail-back" to="/cities">← 返回发现</RouterLink>

  <section class="city-detail-hero">
    <img v-if="cover" :src="cover" :alt="`${cityName}城市风景`" />
    <div class="city-detail-hero-shade" />
    <div class="city-detail-hero-copy">
      <p>{{ city?.province || destination.province || '目的地' }}</p>
      <h1>{{ cityName }}</h1>
      <strong>{{ destination.tag }}</strong>
      <span>{{ city?.description || destination.blurb }}</span>
      <div class="actions">
        <RouterLink class="btn-link btn-coral" :to="planningLink">规划这座城</RouterLink>
        <button
          type="button"
          class="btn-light city-favorite-btn"
          :disabled="!city?.id || busyKey === `city:${city?.id}`"
          @click="favorite"
        >{{ isFavorite('city', city?.id) ? '已收藏' : '收藏城市' }}</button>
      </div>
    </div>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <section class="city-decision-strip" aria-label="城市决策线索">
    <div>
      <span>城市气质</span>
      <strong>{{ destination.tag }}</strong>
    </div>
    <div>
      <span>第一次先看</span>
      <strong>{{ destination.pois.length || 0 }} 个经典地标</strong>
    </div>
    <div>
      <span>图片场景</span>
      <strong v-if="insight">{{ insight.label }} · {{ insight.confidenceText }}</strong>
      <strong v-else>等待更多图片判断</strong>
    </div>
  </section>

  <div class="city-detail-layout">
    <main class="city-detail-main">
      <section>
        <p class="eyebrow">第一次到访</p>
        <h2>先抓住这几处，再决定怎么玩</h2>
        <div v-if="destination.pois.length" class="city-poi-list">
          <article v-for="(poi, index) in destination.pois" :key="poi.name">
            <span>{{ String(index + 1).padStart(2, '0') }}</span>
            <div>
              <h3>{{ poi.name }}</h3>
            </div>
            <RouterLink :to="poiPlanningLink(poi)">围绕它规划 →</RouterLink>
          </article>
        </div>
        <p v-else class="panel-hint">这座城的经典地标还在补充中，可以先带着城市气质去规划。</p>
      </section>

      <section class="city-fit-section">
        <p class="eyebrow">适合你的玩法</p>
        <h2>把城市气质变成明确偏好</h2>
        <div class="city-fit-tags">
          <span v-for="preference in preferences" :key="preference">{{ preference }}</span>
        </div>
      </section>
    </main>

    <aside class="city-detail-aside">
      <p class="eyebrow">下一步</p>
      <h2>不必一次看完所有攻略</h2>
      <RouterLink class="btn-link btn-coral" :to="planningLink">按这些偏好规划</RouterLink>
      <RouterLink class="btn-link btn-ghost" :to="{ path: '/map', query: { city: cityName } }">在立体地图中查看</RouterLink>
      <div v-if="insight" class="city-model-note">
        <strong>本地模型读图</strong>
      </div>
    </aside>
  </div>
</template>
