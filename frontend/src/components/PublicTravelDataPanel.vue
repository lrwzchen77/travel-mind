<script setup>
import { computed } from 'vue';

const props = defineProps({ items: { type: Array, default: () => [] } });

const kinds = {
  live: '实时数据',
  open_data: '公开资料',
  route_estimate: '路线估算',
  demo_reference: '演示参考',
};
const hasWeather = computed(() => props.items.some((item) => item.data_kind === 'live'));

function updated(value) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : `${new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(date)} 查询`;
}
</script>

<template>
  <section v-if="items.length" class="public-data-panel glass-panel" aria-labelledby="public-data-title">
    <div class="section-head">
      <div>
        <p class="eyebrow">免费公开数据快照</p>
        <h2 id="public-data-title">哪些是真的，哪些只是参考</h2>
      </div>
    </div>
    <p v-if="!hasWeather" class="public-data-unavailable">实时天气暂不可用，本次没有用旧天气冒充实时数据。</p>
    <div class="public-data-grid">
      <article v-for="(item, index) in items" :key="`${item.data_kind}-${item.title}-${index}`">
        <div class="public-data-meta">
          <span class="badge" :class="`public-data-kind--${item.data_kind}`">{{ kinds[item.data_kind] || '数据说明' }}</span>
          <span>不可在本平台预订</span>
        </div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.detail }}</p>
        <small>来源：{{ item.source }}<template v-if="updated(item.updated_at)"> · {{ updated(item.updated_at) }}</template></small>
        <a v-if="item.url" :href="item.url" target="_blank" rel="noreferrer">
          {{ item.source?.includes('12306') ? '去 12306 官网核验 →' : '查看数据来源 →' }}
        </a>
      </article>
    </div>
    <p class="public-data-license">
      Open-Meteo 免费接口仅用于本次非商业演示；公开地点来自 OpenStreetMap（ODbL），地图来自 OpenFreeMap / OpenStreetMap。公共服务无可用性保证。
    </p>
  </section>
</template>
