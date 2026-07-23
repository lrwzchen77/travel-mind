<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { resourceApi } from '../api/resources.js';
import { authSession } from '../auth/session.js';
import { cityImageByName } from '../data/cityImages.js';
import VisionInspirationPanel from '../components/VisionInspirationPanel.vue';
import { useFavorites } from '../composables/useFavorites.js';
import { consumerText } from '../data/consumerText.js';

const route = useRoute();
const router = useRouter();
const items = ref([]);
const total = ref(0);
const keyword = ref('');
const loading = ref(false);
const error = ref('');
const { busyKey, isFavorite, loadFavorites, toggleFavorite } = useFavorites();

const resourceKey = computed(() => route.meta.resourceKey);
const title = computed(() => route.meta.title);
const isCityDiscovery = computed(() => resourceKey.value === 'cities');

const resourceMeta = computed(() => {
  const map = {
    cities: {
      emptyTitle: '还没有匹配的城市',
      emptyHint: '换个名字再搜，或直接去规划页写下你想去的地方。',
      searchHint: '搜城市名，例如 杭州 / 成都',
      countLabel: (n) => (n ? `为你找到 ${n} 座城市` : '暂无开放城市'),
      planLabel: '去这里玩',
    },
    attractions: {
      emptyTitle: '没找到合适的景点',
      emptyHint: '试试更短的关键词，或换个城市再看。',
      searchHint: '搜景点名或关键词',
      countLabel: (n) => (n ? `${n} 个值得走进日程的地方` : '暂无景点'),
      planLabel: '排进行程',
    },
    hotels: {
      emptyTitle: '暂时没有合适住宿',
      emptyHint: '换个价位关键词，或在规划时直接写偏好。',
      searchHint: '搜酒店名、风格或地段',
      countLabel: (n) => (n ? `${n} 处可以考虑的落脚点` : '暂无住宿'),
      planLabel: '参考住这里',
    },
    restaurants: {
      emptyTitle: '还没搜到这道味道',
      emptyHint: '试试菜系或地标名，例如「火锅」「西湖醋鱼」。',
      searchHint: '搜菜系、店名或口味',
      countLabel: (n) => (n ? `${n} 家值得专程去吃的店` : '暂无餐厅'),
      planLabel: '吃完再规划',
    },
  };
  return map[resourceKey.value] || map.cities;
});

function subtitle(item) {
  if (resourceKey.value === 'cities') return [item.province, item.country].filter(Boolean).join(' · ') || '目的地';
  if (resourceKey.value === 'restaurants') return [item.cuisine, item.category].filter(Boolean).map(consumerText).join(' · ') || '本地味道';
  return consumerText(item.category || item.tags || '旅行推荐');
}

function detail(item) {
  if (resourceKey.value === 'cities') return item.description || `${item.name}，值得慢慢逛一逛`;
  if (resourceKey.value === 'hotels') {
    const score = item.rating ? `${item.rating} 分` : '评分待补充';
    const price = consumerText(item.price_range) || '价位待确认';
    return `${score} · ${price}`;
  }
  if (resourceKey.value === 'restaurants') {
    const score = item.rating ? `${item.rating} 分` : '评分待补充';
    const cost = item.average_cost != null ? `人均约 ¥${item.average_cost}` : '人均待确认';
    return `${score} · ${cost}`;
  }
  const score = item.rating ? `${item.rating} 分` : '评分待补充';
  const ticket = item.price != null ? `门票约 ¥${item.price}` : '门票待确认';
  return `${score} · ${ticket}`;
}

function coverUrl(item) {
  return item.cover_image || item.image_url || cityImageByName[item.name] || '';
}

function moodClass(item, index) {
  if (coverUrl(item)) return 'has-photo';
  const moods = ['mood-haze', 'mood-spice', 'mood-sea', 'mood-terra'];
  return moods[index % moods.length];
}

function planLink(item) {
  if (resourceKey.value === 'cities') {
    return { path: '/map', query: { city: item.name } };
  }
  const labels = { attractions: '景点', hotels: '住宿', restaurants: '餐厅' };
  const actions = { attractions: '安排游览', hotels: '优先考虑入住', restaurants: '安排用餐' };
  const city = item.city_name || item.city || route.query.city || '';
  const query = {
    resourceType: resourceKey.value,
    resourceName: consumerText(item.name),
    cityId: item.city_id,
    note: `希望${actions[resourceKey.value]}：${consumerText(item.name)}${city ? `（${city}）` : ''}，类型：${labels[resourceKey.value]}。`,
  };
  if (city) query.city = city;
  return { path: '/map', query };
}

function cityDetailLink(item) {
  return isCityDiscovery.value ? `/city/${encodeURIComponent(item.name)}` : undefined;
}

function targetType() {
  const targetTypes = {
    cities: 'city',
    attractions: 'attraction',
    hotels: 'hotel',
    restaurants: 'restaurant',
  };
  return targetTypes[resourceKey.value];
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const [data, cityData] = await Promise.all([
      resourceApi.discover(resourceKey.value, {
        keyword: keyword.value,
        cityId: route.query.cityId || undefined,
        pageSize: resourceKey.value === 'cities' ? 50 : 30,
      }),
      resourceKey.value === 'cities'
        ? Promise.resolve({ records: [] })
        : resourceApi.discover('cities', { pageSize: 100 }).catch(() => ({ records: [] })),
    ]);
    const cityNames = new Map((cityData.records || []).map((city) => [String(city.id), city.name]));
    items.value = (data.records || []).map((item) => ({
      ...item,
      city_name: item.city_name || cityNames.get(String(item.city_id)) || '',
    }));
    total.value = data.total || items.value.length;
  } catch (err) {
    error.value = err?.message || '暂时打不开这份清单，稍后再试';
  } finally {
    loading.value = false;
  }
}

async function loadFavoriteState() {
  try {
    await loadFavorites();
  } catch {
    // Discovery content remains usable if the personal library is temporarily unavailable.
  }
}

async function favorite(item) {
  if (!authSession.isLoggedIn()) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  error.value = '';
  try {
    await toggleFavorite({
      targetType: targetType(),
      targetId: item.id,
      note: item.name,
    });
  } catch (err) {
    error.value = err?.message || '收藏操作没成功，请再试一次';
  }
}

watch(resourceKey, () => {
  keyword.value = '';
  load();
  loadFavoriteState();
});
onMounted(() => {
  load();
  loadFavoriteState();
});
</script>

<template>
  <section class="page-intro">
    <p class="eyebrow">发现</p>
    <h1>{{ title }}</h1>
  </section>

  <form class="discovery-search glass-panel" @submit.prevent="load">
    <div class="discovery-search-field">
      <label class="field-label" for="discover-q">想找什么</label>
      <input
        id="discover-q"
        v-model="keyword"
        :placeholder="resourceMeta.searchHint"
        autocomplete="off"
      />
    </div>
    <button type="submit" class="btn-coral" :disabled="loading">
      {{ loading ? '正在找…' : '搜索' }}
    </button>
  </form>

  <p v-if="error" class="error-line">{{ error }}</p>

  <div class="section-head">
    <div>
      <h2>{{ loading ? '正在翻找灵感…' : resourceMeta.countLabel(total) }}</h2>
    </div>
  </div>

  <div v-if="!loading && !items.length" class="empty-state empty-state--card">
    <strong>{{ resourceMeta.emptyTitle }}</strong>
    <p>{{ resourceMeta.emptyHint }}</p>
    <div class="actions" style="justify-content: center; margin-top: 16px;">
      <RouterLink class="btn-link btn-coral" to="/map">直接去规划</RouterLink>
      <RouterLink class="btn-link btn-ghost" to="/map">先看立体地图</RouterLink>
    </div>
  </div>

  <div v-else class="discovery-grid" :class="{ 'discovery-grid--cities': isCityDiscovery }">
    <article
      v-for="(item, index) in items"
      :key="item.id"
      class="discovery-item"
      :style="{ '--item-index': index }"
    >
      <component
        :is="isCityDiscovery ? RouterLink : 'div'"
        class="discovery-cover"
        :class="moodClass(item, index)"
        :to="cityDetailLink(item)"
        :aria-label="isCityDiscovery ? `查看${item.name}旅行详情` : undefined"
      >
        <img
          v-if="coverUrl(item)"
          class="discovery-cover-image"
          :src="coverUrl(item)"
          :alt="`${item.name}城市风景`"
          loading="lazy"
          decoding="async"
        />
        <span class="discovery-tag">{{ subtitle(item) }}</span>
        <strong>{{ consumerText(item.name) }}</strong>
      </component>
      <div class="discovery-body">
        <p>{{ detail(item) }}</p>
        <div class="discovery-tags" v-if="item.tags">
          <span
            v-for="tag in String(item.tags).split(/[,，、\s]+/).filter(Boolean).slice(0, 3)"
            :key="tag"
            class="chip"
          >{{ consumerText(tag) }}</span>
        </div>
        <div class="discovery-actions">
          <button
            type="button"
            class="text-action"
            :disabled="busyKey === `${targetType()}:${item.id}`"
            @click="favorite(item)"
          >
            <template v-if="busyKey === `${targetType()}:${item.id}`">处理中…</template>
            <template v-else-if="isFavorite(targetType(), item.id)">已收藏</template>
            <template v-else>收藏</template>
          </button>
          <RouterLink class="text-action text-action--primary" :to="planLink(item)">
            {{ resourceMeta.planLabel }} →
          </RouterLink>
        </div>
      </div>
    </article>
  </div>

  <details v-if="isCityDiscovery" class="vision-disclosure glass-panel">
    <summary>
      <span>按照片找旅行感觉</span>
      <small>有一张喜欢的风景照？从照片里的氛围开始找目的地。</small>
    </summary>
    <VisionInspirationPanel />
  </details>
</template>
