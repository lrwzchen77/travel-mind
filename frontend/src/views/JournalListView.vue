<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ArrowRight, BookOpen } from 'lucide-vue-next';
import { journalApi, journalImageUrl } from '../api/journal.js';
import PagePrologue from '../components/PagePrologue.vue';
import { useReveal } from '../composables/useReveal.js';

const root = ref(null);
useReveal(root);

const journals = ref([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const error = ref('');
const hasMore = computed(() => journals.value.length < total.value);

function updatedAt(value) {
  const match = String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})/);
  return match ? `${Number(match[2])}月${Number(match[3])}日更新` : '刚刚创建';
}

async function load(pageNum = 1) {
  loading.value = true;
  error.value = '';
  try {
    const data = await journalApi.list({ pageNum, pageSize: 20 });
    journals.value = pageNum === 1 ? (data.records || []) : [...journals.value, ...(data.records || [])];
    total.value = data.total || journals.value.length;
    page.value = pageNum;
  } catch (err) {
    error.value = err?.message || '暂时打不开旅行游记。';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div ref="root" class="memory-list-page">
    <PagePrologue index="07 · 游记" eyebrow="旅行游记" title="把走过的路，写成自己的故事" lead="从行程生成游记，添加照片和地点，记录每一段旅程的独特记忆。" next-label="规划新行程" next-to="/map" />

    <p v-if="error" class="error-line" role="alert">{{ error }}</p>
    <div class="section-head" data-reveal style="--reveal-delay: 0s">
      <div><h2>{{ loading ? '正在打开旅行游记…' : (journals.length ? `${journals.length} 篇旅行游记` : '还没有旅行游记') }}</h2></div>
      <RouterLink class="btn-link btn-coral" to="/trip-history">从已有行程开始</RouterLink>
    </div>

    <div v-if="!loading && !journals.length" class="empty-state empty-state--card memory-empty" data-reveal style="--reveal-delay: 0.08s">
      <strong>还没有旅行游记</strong>
      <p>从一趟已有行程开始，把照片和去过的地方写成一篇游记。</p>
      <RouterLink class="btn-link btn-coral" to="/trip-history">从已有行程开始</RouterLink>
    </div>

    <div v-else class="memory-grid" data-reveal style="--reveal-delay: 0.08s" aria-live="polite">
      <RouterLink v-for="entry in journals" :key="entry.id" :to="`/journals/${entry.id}`" class="memory-card">
        <div class="memory-card-cover">
          <img v-if="entry.coverImage" :src="journalImageUrl(entry.coverImage)" :alt="entry.title || `${entry.destinationCity || '旅行'}游记`" loading="lazy" />
          <span v-else aria-hidden="true"><BookOpen :size="32" /></span>
          <em>{{ entry.visibility === 'public' ? '公开' : '私密' }}</em>
        </div>
        <div class="memory-card-body">
          <p class="memory-card-kicker">{{ entry.destinationCity || '目的地待补充' }} · {{ updatedAt(entry.updateTime) }}</p>
          <h2>{{ entry.title || '未命名游记' }}</h2>
          <p>{{ entry.summary || '把照片和旅途片段按天整理，写成属于你的旅行故事。' }}</p>
          <p class="memory-card-action">{{ entry.travelDays || 1 }} 天 · {{ entry.viewCount || 0 }} 次浏览</p>
        </div>
      </RouterLink>
    </div>

    <div v-if="hasMore" class="load-more">
      <button type="button" class="btn-ghost" :disabled="loading" @click="load(page + 1)">{{ loading ? '正在加载…' : `加载更多（还有 ${total - journals.length} 篇）` }}</button>
    </div>

    <section class="chapter-bridge" data-reveal style="--reveal-delay: 0.16s">
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章</p>
        <h2 class="chapter-bridge-title">写完游记，再去一次</h2>
        <p class="chapter-bridge-lead">从过去的游记里挑一个想去的地方，重新规划下一程。</p>
      </div>
      <RouterLink class="chapter-bridge-cta" to="/planning">
        <span>去规划</span>
        <ArrowRight :size="18" :stroke-width="2.2" />
      </RouterLink>
    </section>
  </div>
</template>
