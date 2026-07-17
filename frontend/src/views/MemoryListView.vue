<script setup>
import { onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { memoryApi, memoryImageUrl } from '../api/memory.js';

const memories = ref([]);
const loading = ref(false);
const error = ref('');

const generationLabel = { pending: '待整理', processing: '整理中', ready: '时间线已生成', failed: '整理失败' };
const indexLabel = { pending: '待建立知识索引', indexing: '索引中', ready: '可问答', unavailable: '问答暂不可用', failed: '索引失败' };

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const data = await memoryApi.list({ pageSize: 30 });
    memories.value = data.records || [];
  } catch (err) {
    error.value = err?.message || '暂时打不开旅行记忆。';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-intro memory-list-intro">
    <p class="eyebrow">我的旅行记忆</p>
    <h1>把走过的路，整理成可以追问的回忆</h1>
    <p class="lead">照片、地点和真实支出只在你的私有记忆册中整理；由你决定是否另做公开分享。</p>
  </section>

  <p v-if="error" class="error-line" role="alert">{{ error }}</p>
  <div class="section-head">
    <div><h2>{{ loading ? '正在翻找回忆…' : `${memories.length} 本记忆册` }}</h2></div>
    <RouterLink class="btn-link btn-coral" to="/trip-history">从一趟行程开始</RouterLink>
  </div>

  <div v-if="!loading && !memories.length" class="empty-state empty-state--card memory-empty">
    <strong>还没有旅行记忆</strong>
    <p>打开一趟已经保存的行程，点“生成旅行回忆”，第一本记忆册就会出现。</p>
    <RouterLink class="btn-link btn-coral" to="/trip-history">打开我的行程</RouterLink>
  </div>

  <div v-else class="memory-grid" aria-live="polite">
    <RouterLink v-for="memory in memories" :key="memory.id" :to="`/memories/${memory.id}`" class="memory-card">
      <div class="memory-card-cover">
        <img v-if="memory.cover_image" :src="memoryImageUrl(memory.cover_image)" :alt="memory.title" loading="lazy" />
        <span v-else aria-hidden="true">{{ memory.destination_city || '旅行' }}</span>
        <em>仅自己可见</em>
      </div>
      <div class="memory-card-body">
        <p class="memory-card-kicker">{{ memory.destination_city || '目的地' }} · {{ memory.item_count || 0 }} 条证据</p>
        <h2>{{ memory.title || '未命名旅行记忆' }}</h2>
        <p>{{ memory.summary || '照片和行程事实会在这里按天整理。' }}</p>
        <div class="memory-card-status">
          <span>{{ generationLabel[memory.generation_status] || memory.generation_status }}</span>
          <span>{{ indexLabel[memory.index_status] || memory.index_status }}</span>
        </div>
      </div>
    </RouterLink>
  </div>
</template>
