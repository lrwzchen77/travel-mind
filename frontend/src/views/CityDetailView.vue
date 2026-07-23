<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { resourceApi } from '../api/resources.js';
import { cityImageByName } from '../data/cityImages.js';
import { findDestination } from '../data/geoDestinations.js';
import { useFavorites } from '../composables/useFavorites.js';
import { consumerText } from '../data/consumerText.js';
import { supportsPlanning } from '../data/planningSupport.js';

const route = useRoute();
const router = useRouter();
const cityName = computed(() => String(route.params.city || '').trim());
const city = ref(null);
const loading = ref(true);
const error = ref('');
const resources = ref({ attractions: [], hotels: [], restaurants: [] });
const destination = computed(() => findDestination(cityName.value));
const cover = computed(() => cityImageByName[cityName.value] || '');
const { busyKey, isFavorite, loadFavorites, toggleFavorite } = useFavorites();
const resourceCount = computed(() => Object.values(resources.value).reduce((sum, items) => sum + items.length, 0));
const planningAvailable = computed(() => supportsPlanning(cityName.value));

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
  path: '/map',
  query: { city: cityName.value, preferences: preferences.value.join(',') },
}));

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const data = await resourceApi.discover('cities', { keyword: cityName.value, pageSize: 50 });
    city.value = (data.records || []).find((item) => item.name === cityName.value) || null;
    if (city.value?.id) {
      const keys = ['attractions', 'hotels', 'restaurants'];
      const results = await Promise.allSettled(keys.map((key) => resourceApi.discover(key, {
        cityId: city.value.id,
        pageSize: 12,
      })));
      resources.value = Object.fromEntries(keys.map((key, index) => [
        key,
        results[index].status === 'fulfilled' ? results[index].value.records || [] : [],
      ]));
    }
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

function resourcePlanningLink(item, type) {
  const actions = { attractions: '安排游览', hotels: '优先考虑入住', restaurants: '安排用餐' };
  return {
    path: '/map',
    query: {
      city: cityName.value,
      cityId: city.value?.id,
      resourceType: type,
      resourceName: consumerText(item.name),
      note: `希望${actions[type]}：${consumerText(item.name)}（${cityName.value}）。`,
    },
  };
}

function resourceFacts(item, type) {
  if (type === 'attractions') return [
    consumerText(item.address),
    item.price != null ? `门票约 ¥${item.price}` : '',
    item.opening_hours ? `开放时间 ${consumerText(item.opening_hours)}` : '',
  ].filter(Boolean);
  if (type === 'hotels') return [item.address, item.price_range].filter(Boolean).map(consumerText);
  return [consumerText(item.address), item.average_cost != null ? `人均约 ¥${item.average_cost}` : ''].filter(Boolean);
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
        <RouterLink v-if="planningAvailable" class="btn-link btn-coral" :to="planningLink">规划这座城</RouterLink>
        <span v-else class="btn-link btn-light" aria-disabled="true">完整规划待开放</span>
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
  <p v-if="!planningAvailable" class="trust-note">这座城当前可浏览城市灵感，但吃、住、玩真实资源尚未补齐，因此暂不开放完整规划，避免生成占位商户。</p>

  <section class="city-decision-strip" aria-label="城市决策线索">
    <div>
      <span>所在地区</span>
      <strong>{{ [city?.province || destination.province, city?.country].filter(Boolean).join(' · ') || '地区待补充' }}</strong>
    </div>
    <div>
      <span>城内旅行信息</span>
      <strong>{{ resourceCount ? `${resourceCount} 条吃住玩线索` : '内容持续补充中' }}</strong>
    </div>
    <div>
      <span>适合的旅行感觉</span>
      <strong>{{ preferences.join(' · ') }}</strong>
    </div>
  </section>

  <div class="city-detail-layout">
    <main class="city-detail-main">
      <section class="city-resource-guide">
        <p class="eyebrow">城内旅行清单</p>
        <h2>先看去哪玩，再决定吃哪、住哪</h2>
        <nav aria-label="城市旅行信息分区">
          <a href="#city-attractions">去哪玩</a>
          <a href="#city-restaurants">吃什么</a>
          <a href="#city-hotels">住哪里</a>
        </nav>
      </section>

      <section
        v-for="section in [
          { key: 'attractions', id: 'city-attractions', eyebrow: '去哪玩', title: `${cityName}值得排进日程的地方` },
          { key: 'restaurants', id: 'city-restaurants', eyebrow: '吃什么', title: `${cityName}的本地味道` },
          { key: 'hotels', id: 'city-hotels', eyebrow: '住哪里', title: `${cityName}的落脚选择` },
        ]"
        :id="section.id"
        :key="section.key"
        class="city-resource-section"
      >
        <p class="eyebrow">{{ section.eyebrow }}</p>
        <h2>{{ section.title }}</h2>
        <div v-if="resources[section.key].length" class="city-resource-list">
          <article v-for="item in resources[section.key]" :key="item.id">
            <div>
              <h3>{{ consumerText(item.name) }}</h3>
              <p v-if="item.description">{{ consumerText(item.description) }}</p>
              <ul v-if="resourceFacts(item, section.key).length">
                <li v-for="fact in resourceFacts(item, section.key)" :key="fact">{{ fact }}</li>
              </ul>
            </div>
            <RouterLink v-if="planningAvailable" :to="resourcePlanningLink(item, section.key)">带去规划 →</RouterLink>
            <span v-else class="panel-hint">信息仅供浏览</span>
          </article>
        </div>
        <p v-else class="panel-hint">这部分信息还在补充，可以在规划时直接写下你的要求。</p>
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
      <RouterLink v-if="planningAvailable" class="btn-link btn-coral" :to="planningLink">按这些偏好规划</RouterLink>
      <span v-else class="btn-link btn-ghost" aria-disabled="true">完整规划待开放</span>
      <RouterLink class="btn-link btn-ghost" :to="{ path: '/map', query: { city: cityName } }">在地图情报中查看</RouterLink>
    </aside>
  </div>
</template>
