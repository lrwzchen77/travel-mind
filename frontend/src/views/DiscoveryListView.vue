<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { ArrowRight } from 'lucide-vue-next';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { resourceApi } from '../api/resources.js';
import { authSession } from '../auth/session.js';
import { cityImageByName } from '../data/cityImages.js';
import PagePrologue from '../components/PagePrologue.vue';
import VisionInspirationPanel from '../components/VisionInspirationPanel.vue';
import { useFavorites } from '../composables/useFavorites.js';
import { consumerText } from '../data/consumerText.js';

const route = useRoute();
const router = useRouter();
const items = ref([]);
const total = ref(0);
const page = ref(1);
const keyword = ref('');
const category = ref('');
const tag = ref('');
const ratingMin = ref('');
const availableTags = ref([]);
const loading = ref(false);
const error = ref('');
const { busyKey, isFavorite, loadFavorites, toggleFavorite } = useFavorites();

const resourceKey = computed(() => route.meta.resourceKey);
const title = computed(() => route.meta.title);
const isCityDiscovery = computed(() => resourceKey.value === 'cities');
const hasMore = computed(() => items.value.length < total.value);

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
    const price = item.cost != null ? `参考 ¥${item.cost}` : consumerText(item.price_range) || '价位待确认';
    return `${score} · ${price}`;
  }
  if (resourceKey.value === 'restaurants') {
    const score = item.rating ? `${item.rating} 分` : '评分待补充';
    const costValue = item.cost ?? item.average_cost;
    const cost = costValue != null ? `人均约 ¥${costValue}` : '人均待确认';
    return `${score} · ${cost}`;
  }
  const score = item.rating ? `${item.rating} 分` : '评分待补充';
  const ticketValue = item.cost ?? item.price;
  const ticket = ticketValue != null ? `门票约 ¥${ticketValue}` : '门票待确认';
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

function detailLink(item) {
  return isCityDiscovery.value
    ? `/city/${encodeURIComponent(item.name)}`
    : `/discover/${resourceKey.value}/${item.id}`;
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

async function load(pageNum = 1) {
  loading.value = true;
  error.value = '';
  try {
    const [data, cityData] = await Promise.all([
      resourceApi.discover(resourceKey.value, {
        keyword: keyword.value,
        cityId: route.query.cityId || undefined,
        category: category.value || undefined,
        tag: tag.value || undefined,
        ratingMin: ratingMin.value || undefined,
        pageNum,
        pageSize: resourceKey.value === 'cities' ? 50 : 30,
      }),
      resourceKey.value === 'cities'
        ? Promise.resolve({ records: [] })
        : resourceApi.discover('cities', { pageSize: 100 }).catch(() => ({ records: [] })),
    ]);
    const cityNames = new Map((cityData.records || []).map((city) => [String(city.id), city.name]));
    const records = (data.records || []).map((item) => ({
      ...item,
      city_name: item.city_name || cityNames.get(String(item.city_id)) || '',
    }));
    items.value = pageNum === 1 ? records : [...items.value, ...records];
    page.value = pageNum;
    total.value = data.total || items.value.length;
  } catch (err) {
    error.value = err?.message || '暂时打不开这份清单，稍后再试';
  } finally {
    loading.value = false;
  }
}

async function loadTags() {
  try {
    const data = await resourceApi.discover('travel-tags', { pageSize: 100 });
    availableTags.value = data.records || [];
  } catch {
    availableTags.value = [];
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
  category.value = '';
  tag.value = '';
  ratingMin.value = '';
  load(1);
  loadFavoriteState();
});
onMounted(() => {
  load(1);
  loadTags();
  loadFavoriteState();
});
</script>

<template>
  <PagePrologue index="04" eyebrow="发现" :title="title" lead="挑一座城，把它拆成玩、住、吃三个清单。" next-label="回到地图" next-to="/map" />

  <form class="discovery-search glass-panel" @submit.prevent="load(1)">
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
    <div v-if="!isCityDiscovery" class="discovery-filter-row">
      <label><span class="field-label">分类</span><input v-model="category" placeholder="例如自然风光、精品住宿" /></label>
      <label><span class="field-label">标签</span><select v-model="tag"><option value="">全部标签</option><option v-for="item in availableTags" :key="item.id" :value="item.name">{{ item.name }}</option></select></label>
      <label><span class="field-label">最低评分</span><select v-model="ratingMin"><option value="">不限评分</option><option value="4">4.0 分以上</option><option value="4.5">4.5 分以上</option></select></label>
    </div>
  </form>

  <p v-if="error" class="error-line">{{ error }}</p>

  <div class="section-head">
    <div>
      <h2>{{ loading ? '正在翻找灵感…' : resourceMeta.countLabel(total) }}</h2>
    </div>
  </div>
  <div v-if="hasMore" class="load-more"><button type="button" class="btn-ghost" :disabled="loading" @click="load(page + 1)">{{ loading ? '正在加载…' : `加载更多（还有 ${total - items.length} 项）` }}</button></div>

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
        :is="RouterLink"
        class="discovery-cover"
        :class="moodClass(item, index)"
        :to="detailLink(item)"
        :aria-label="`查看${item.name}${isCityDiscovery ? '旅行' : ''}详情`"
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
            {{ resourceMeta.planLabel }} <ArrowRight :size="15" :stroke-width="2.2" />
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

  <section class="chapter-bridge" data-reveal>
    <div class="chapter-bridge-copy">
      <p class="chapter-bridge-eyebrow">下一章 · 02 地图</p>
      <h2 class="chapter-bridge-title">挑完城市，去地图圈地点</h2>
      <p class="chapter-bridge-lead">资源页帮你找到想去的，地图负责把它们串成走得通的路线。打开立体地图，开始圈点。</p>
    </div>
    <RouterLink class="chapter-bridge-cta" to="/map">
      <span>打开立体地图</span>
      <ArrowRight :size="18" :stroke-width="2.2" />
    </RouterLink>
  </section>
</template>

<style scoped>
.discovery-filter-row { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
@media (max-width: 720px) { .discovery-filter-row { grid-template-columns: 1fr; } }
</style>
