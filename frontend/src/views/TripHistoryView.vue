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
  return statusMap[status] || status || '行程';
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const data = await tripApi.history(20);
    items.value = data.items || [];
    total.value = data.total || items.value.length;
  } catch (err) {
    error.value = err?.message || '暂时加载不了行程，检查网络或后端后再试';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-intro">
    <p class="eyebrow">我的行程</p>
    <h1>你走过的、计划中的</h1>
    <p>每一程都像一张旅行明信片。点进去可以复制、删除，或继续和行程聊聊。</p>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <div class="section-head">
    <div>
      <h2>{{ loading ? '加载中…' : `${total} 份行程` }}</h2>
      <p>从最新的开始看起</p>
    </div>
    <div class="actions">
      <button type="button" class="btn-ghost btn-sm" @click="load">刷新</button>
      <RouterLink class="btn-link btn-coral" style="min-height: 36px; padding: 0 16px; font-size: 13px;" to="/planning">
        新规划一程
      </RouterLink>
    </div>
  </div>

  <div v-if="!loading && items.length === 0" class="glass-panel">
    <div class="empty-state">
      <strong>行李箱还是空的</strong>
      还没有保存的行程。去规划一趟吧——哪怕只是周末两天。
      <div class="actions" style="justify-content: center; margin-top: 18px;">
        <RouterLink class="btn-link btn-coral" to="/planning">开始规划</RouterLink>
      </div>
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
        <span>#{{ item.id }}</span>
        <h3>{{ item.destination_city || item.title || '未命名目的地' }}</h3>
      </div>
      <div class="trip-card-body">
        <p>{{ item.title || '智能规划行程' }}</p>
        <p>{{ item.start_date }} — {{ item.end_date }}</p>
        <div class="trip-card-foot">
          <span class="badge badge-muted">{{ statusLabel(item.status) }}</span>
          <span style="font-size: 13px; color: var(--ocean); font-weight: 700;">查看详情 →</span>
        </div>
      </div>
    </RouterLink>
  </div>
</template>
