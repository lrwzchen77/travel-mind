<script setup>
import { onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { tripApi } from '../api/trip.js';

const items = ref([]);
const total = ref(0);
const error = ref('');
const loading = ref(false);

const statusMap = {
  COMPLETED: '已完成',
  DRAFT: '草稿',
  SAVED: '已保存',
  ACTIVE: '进行中',
  ARCHIVED: '已归档',
};

function statusLabel(status) {
  return statusMap[status] || '行程';
}

function statusClass(status) {
  if (status === 'COMPLETED' || status === 'SAVED') return 'badge-ok';
  if (status === 'DRAFT' || status === 'ACTIVE') return 'badge-warn';
  return 'badge-muted';
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const data = await tripApi.history(20);
    items.value = data.items || [];
    total.value = data.total || items.value.length;
  } catch (err) {
    error.value = err?.message || '暂时打不开行程册，检查网络后再试';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-intro">
    <p class="eyebrow">我的行程</p>
    <h1>走过的，和还想去的</h1>
    <p>每一程都像一张旅行明信片。点进去可以继续微调、问问预算，或复制一程再出发。</p>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <div class="section-head">
    <div>
      <h2>{{ loading ? '正在翻开行程册…' : (total ? `${total} 份行程` : '行程册空着') }}</h2>
      <p>从最近一次开始看</p>
    </div>
    <RouterLink
      class="btn-link btn-coral"
      style="min-height: 40px; padding: 0 18px; font-size: 13px;"
      to="/planning"
    >
      新规划一程
    </RouterLink>
  </div>

  <div v-if="!loading && items.length === 0" class="empty-state empty-state--card">
    <strong>行李箱还是空的</strong>
    <p>还没有保存的行程。去规划一趟吧——哪怕只是周末两天。</p>
    <div class="actions" style="justify-content: center; margin-top: 18px;">
      <RouterLink class="btn-link btn-coral" to="/planning">开始规划</RouterLink>
      <RouterLink class="btn-link btn-ghost" to="/cities">先找灵感</RouterLink>
    </div>
  </div>

  <div v-else class="trip-grid">
    <RouterLink
      v-for="(item, index) in items"
      :key="item.id"
      :to="`/trip/${item.id}`"
      class="trip-card"
      :style="{ animationDelay: `${index * 60}ms` }"
    >
      <div class="trip-card-banner">
        <span>{{ item.travel_days ? `${item.travel_days} 天` : '旅行计划' }}</span>
        <h3>{{ item.destination_city || item.title || '未命名目的地' }}</h3>
      </div>
      <div class="trip-card-body">
        <p class="trip-card-title">{{ item.title || '智能规划行程' }}</p>
        <p>
          <template v-if="item.start_date || item.end_date">
            {{ item.start_date || '待定' }} — {{ item.end_date || '待定' }}
          </template>
          <template v-else>日期待定</template>
        </p>
        <div class="trip-card-foot">
          <span class="badge" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
          <span class="trip-card-go">打开 →</span>
        </div>
      </div>
    </RouterLink>
  </div>
</template>
