<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ArrowRight } from 'lucide-vue-next';
import { memoryApi, memoryImageUrl } from '../api/memory.js';
import PagePrologue from '../components/PagePrologue.vue';
import { useReveal } from '../composables/useReveal.js';

const root = ref(null);
useReveal(root);
const memories = ref([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const error = ref('');
const hasMore = computed(() => memories.value.length < total.value);

function actionHint(memory) {
  if (memory.generation_status === 'failed') return '上次整理没有完成，打开后可以重新整理';
  if (['failed', 'unavailable'].includes(memory.index_status)) return '记录已更新，暂时不能查找旅行细节';
  if (memory.generation_status === 'pending') return '这本记录有新内容，打开后更新一下';
  if (memory.index_status === 'pending') return '打开后可以准备查找旅行细节';
  return '';
}

function updatedAt(value) {
  const match = String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})/);
  return match ? `${Number(match[2])}月${Number(match[3])}日更新` : '等待第一次更新';
}

async function load(pageNum = 1) {
  loading.value = true;
  error.value = '';
  try {
    const data = await memoryApi.list({ pageNum, pageSize: 30 });
    memories.value = pageNum === 1 ? (data.records || []) : [...memories.value, ...(data.records || [])];
    total.value = data.total || memories.value.length;
    page.value = pageNum;
  } catch (err) {
    error.value = err?.message || '暂时打不开旅行记录。';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div ref="root" class="memory-list-page">
    <PagePrologue index="07" eyebrow="旅行记录" title="把每一程，留在真实走过的日子里" lead="照片、去过的地方和旅途片段会按天放在一起，默认只有你能看到。" next-label="规划新行程" next-to="/map" />

    <p v-if="error" class="error-line" role="alert">{{ error }}</p>
    <div class="section-head" data-reveal style="--reveal-delay: 0s">
      <div><h2>{{ loading ? '正在打开旅行记录…' : (memories.length ? `${memories.length} 篇旅行记录` : '还没有旅行记录') }}</h2></div>
      <RouterLink class="btn-link btn-coral" to="/trip-history">从已有行程开始</RouterLink>
    </div>

    <div v-if="!loading && !memories.length" class="empty-state empty-state--card memory-empty" data-reveal style="--reveal-delay: 0.08s">
      <strong>还没有旅行记录</strong>
      <p>从一趟已有行程开始，照片和去过的地方会按天整理在一起。</p>
      <RouterLink class="btn-link btn-coral" to="/trip-history">从已有行程开始</RouterLink>
    </div>

    <div v-else class="memory-grid" data-reveal style="--reveal-delay: 0.08s" aria-live="polite">
    <RouterLink v-for="entry in memories" :key="entry.id" :to="`/memories/${entry.id}`" class="memory-card">
      <div class="memory-card-cover">
        <img v-if="entry.cover_image" :src="memoryImageUrl(entry.cover_image)" :alt="entry.title || `${entry.destination_city || '旅行'}照片`" loading="lazy" />
        <span v-else aria-hidden="true">{{ entry.destination_city || '旅行' }}</span>
        <em>私密</em>
      </div>
      <div class="memory-card-body">
        <p class="memory-card-kicker">{{ entry.destination_city || '目的地待补充' }} · {{ updatedAt(entry.update_time) }}</p>
        <h2>{{ entry.title || '未命名旅行记录' }}</h2>
        <p>{{ entry.summary || '照片和旅途片段会在这里按天放在一起。' }}</p>
        <p v-if="actionHint(entry)" class="memory-card-action">{{ actionHint(entry) }}</p>
      </div>
    </RouterLink>
  </div>
  <div v-if="hasMore" class="load-more"><button type="button" class="btn-ghost" :disabled="loading" @click="load(page + 1)">{{ loading ? '正在加载…' : `加载更多（还有 ${total - memories.length} 篇）` }}</button></div>

    <section class="chapter-bridge" data-reveal style="--reveal-delay: 0.16s">
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章</p>
        <h2 class="chapter-bridge-title">回忆写完，再去一次</h2>
        <p class="chapter-bridge-lead">从过去的记录里挑一个想去的地方，重新规划下一程。</p>
      </div>
      <RouterLink class="chapter-bridge-cta" to="/planning">
        <span>去规划</span>
        <ArrowRight :size="18" :stroke-width="2.2" />
      </RouterLink>
    </section>
  </div>
</template>
