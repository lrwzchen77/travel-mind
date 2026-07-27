<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { ArrowLeft, ArrowRight, MapPin } from 'lucide-vue-next';
import { RouterLink, useRoute } from 'vue-router';
import { resourceApi } from '../api/resources.js';
import { cityImageByName } from '../data/cityImages.js';
import { consumerText } from '../data/consumerText.js';

const route = useRoute();
const item = ref(null);
const cityName = ref('');
const loading = ref(false);
const error = ref('');
const resourceKey = computed(() => String(route.params.resourceKey || ''));
const meta = computed(() => ({
  attractions: { label: '景点', action: '安排游览' },
  hotels: { label: '住宿', action: '优先考虑入住' },
  restaurants: { label: '餐厅', action: '安排用餐' },
}[resourceKey.value] || { label: '旅行地点', action: '加入行程' }));
const cover = computed(() => item.value?.cover_image || item.value?.image_url || cityImageByName[item.value?.name] || '');
const tags = computed(() => String(item.value?.tags || '').split(/[,，、\s]+/).filter(Boolean));
const location = computed(() => item.value?.address || cityName.value || '位置以实际导航为准');
const facts = computed(() => [
  item.value?.rating != null ? `${item.value.rating} 分` : '',
  item.value?.category ? consumerText(item.value.category) : '',
  item.value?.cuisine ? consumerText(item.value.cuisine) : '',
  item.value?.cost != null ? `参考 ¥${item.value.cost}` : '',
  item.value?.average_cost != null ? `人均约 ¥${item.value.average_cost}` : '',
].filter(Boolean));
const planLink = computed(() => ({
  path: '/map',
  query: {
    city: cityName.value || undefined,
    resourceType: resourceKey.value,
    resourceName: consumerText(item.value?.name),
    cityId: item.value?.city_id,
    note: `希望${meta.value.action}：${consumerText(item.value?.name)}，类型：${meta.value.label}。`,
  },
}));

async function load() {
  loading.value = true;
  error.value = '';
  cityName.value = '';
  try {
    item.value = await resourceApi.discoverDetail(resourceKey.value, route.params.id);
    cityName.value = item.value?.city_name || item.value?.city || '';
    if (!cityName.value && item.value?.city_id) {
      try {
        const city = await resourceApi.discoverDetail('cities', item.value.city_id);
        cityName.value = city?.name || '';
      } catch {
        // The resource detail remains useful without the optional city label.
      }
    }
  } catch (err) {
    error.value = err?.message || '这条旅行信息暂时不可用。';
  } finally {
    loading.value = false;
  }
}

watch(() => [route.params.resourceKey, route.params.id], load);
onMounted(load);
</script>

<template>
  <RouterLink class="city-detail-back" :to="`/${resourceKey}`"><ArrowLeft :size="15" :stroke-width="2.2" /> 返回{{ meta.label }}清单</RouterLink>
  <p v-if="error" class="error-line" role="alert">{{ error }}</p>
  <div v-else-if="loading" class="empty-state">正在打开详情…</div>
  <article v-else-if="item" class="resource-detail">
    <div class="resource-detail-visual">
      <img v-if="cover" :src="cover" :alt="`${item.name}${meta.label}参考图`" decoding="async" referrerpolicy="no-referrer" />
      <span v-else>{{ meta.label }}</span>
      <div><p class="eyebrow">{{ meta.label }}详情</p><h1>{{ consumerText(item.name) }}</h1></div>
    </div>
    <div class="resource-detail-copy glass-panel">
      <p class="resource-detail-location"><MapPin :size="16" :stroke-width="2.2" /> {{ location }}</p>
      <div v-if="facts.length" class="resource-detail-facts"><strong v-for="fact in facts" :key="fact">{{ fact }}</strong></div>
      <p class="resource-detail-description">{{ consumerText(item.description || item.detail || item.introduction || '详细开放时间、价格和预订要求请在出发前再次确认。') }}</p>
      <div v-if="tags.length" class="chip-row"><span v-for="tag in tags" :key="tag" class="chip">{{ consumerText(tag) }}</span></div>
      <div class="actions">
        <RouterLink class="btn-link btn-coral" :to="planLink">{{ meta.action }} <ArrowRight :size="15" :stroke-width="2.2" /></RouterLink>
        <RouterLink class="btn-link btn-ghost" :to="`/${resourceKey}`">继续浏览</RouterLink>
      </div>
    </div>
  </article>
</template>

<style scoped>
.resource-detail { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(300px, .75fr); gap: 24px; margin-top: 18px; }
.resource-detail-visual { position: relative; min-height: 520px; overflow: hidden; border-radius: var(--radius-lg); background: var(--tm-paper-muted); }
.resource-detail-visual img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.resource-detail-visual > span { display: grid; min-height: 520px; place-items: center; color: var(--tm-muted); font: 800 clamp(48px, 9vw, 120px)/1 var(--font-display); opacity: .28; }
.resource-detail-visual::after { position: absolute; inset: 45% 0 0; background: linear-gradient(transparent, rgba(8, 10, 14, .86)); content: ''; }
.resource-detail-visual > div { position: absolute; z-index: 1; right: 0; bottom: 0; left: 0; padding: clamp(24px, 5vw, 52px); color: #fff; }
.resource-detail-visual h1 { max-width: 12ch; margin: 6px 0 0; font: 800 clamp(36px, 6vw, 76px)/.98 var(--font-display); }
.resource-detail-copy { align-self: end; padding: clamp(22px, 3vw, 34px); }
.resource-detail-location { display: flex; align-items: center; gap: 7px; color: var(--tm-muted); }
.resource-detail-facts { display: flex; flex-wrap: wrap; gap: 8px; margin: 22px 0; }
.resource-detail-facts strong { padding: 7px 10px; border: 1px solid var(--tm-line); border-radius: var(--tm-radius-pill); font-size: 12px; }
.resource-detail-description { color: var(--tm-ink-soft); line-height: 1.8; white-space: pre-line; }
.resource-detail-copy .actions { margin-top: 28px; }
@media (max-width: 820px) { .resource-detail { grid-template-columns: 1fr; } .resource-detail-visual, .resource-detail-visual > span { min-height: 420px; } }
</style>
