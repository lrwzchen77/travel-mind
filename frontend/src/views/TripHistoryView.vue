<script setup>
import { computed, onMounted, ref } from 'vue';
import { ArrowRight, Plus } from 'lucide-vue-next';
import { RouterLink } from 'vue-router';
import { tripApi } from '../api/trip.js';
import { cityImageByName } from '../data/cityImages.js';
import PagePrologue from '../components/PagePrologue.vue';
import { useReveal } from '../composables/useReveal.js';

const root = ref(null);
useReveal(root);
const items = ref([]);
const total = ref(0);
const error = ref('');
const loading = ref(false);
const limit = ref(20);
const hasMore = computed(() => items.value.length < total.value);

const statusMap = {
  COMPLETED: '已结束',
  DRAFT: '待完善',
  SAVED: '已规划',
  ACTIVE: '旅行中',
  ARCHIVED: '已归档',
};

function statusLabel(status) {
  return statusMap[String(status || '').toUpperCase()] || '已规划';
}

function statusClass(status) {
  status = String(status || '').toUpperCase();
  if (status === 'COMPLETED' || status === 'SAVED') return 'badge-ok';
  if (status === 'DRAFT' || status === 'ACTIVE') return 'badge-warn';
  return 'badge-muted';
}

function tripCover(item) {
  return cityImageByName[String(item.destination_city || '').replace(/市$/, '')] || '';
}

async function load(nextLimit = limit.value) {
  loading.value = true;
  error.value = '';
  try {
    const data = await tripApi.history(nextLimit);
    items.value = data.items || [];
    total.value = data.total || items.value.length;
    limit.value = nextLimit;
  } catch (err) {
    error.value = err?.message || '暂时打不开行程册，检查网络后再试';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div ref="root" class="journey-album-page">
    <section class="journey-masthead" data-reveal style="--reveal-delay: 0s">
      <PagePrologue index="06" eyebrow="我的行程 · 私人行程册">
        <template #title>走过的，<em>和还想去的。</em></template>
        <template #lead>每一张都是一段准备出发或已经发生的旅程。</template>
      </PagePrologue>
      <RouterLink class="btn-link btn-coral" to="/map"><Plus :size="15" :stroke-width="2.2" /> 新规划一程</RouterLink>
    </section>

    <p v-if="error" class="error-line">{{ error }}</p>

    <div class="journey-album-head" data-reveal style="--reveal-delay: 0.08s">
      <div><span>按保存时间排列</span><h2>{{ loading ? '正在翻开行程册…' : (total ? `${total} 份行程` : '行程册空着') }}</h2></div>
      <RouterLink class="text-link" to="/cities">去找下一站 <ArrowRight :size="15" :stroke-width="2.2" /></RouterLink>
    </div>

    <div v-if="!loading && items.length === 0" class="empty-state empty-state--card" data-reveal style="--reveal-delay: 0.16s">
      <strong>行李箱还是空的</strong>
      <p>还没有保存的行程。去规划一趟吧——哪怕只是周末两天。</p>
      <div class="actions" style="justify-content: center; margin-top: 18px;">
        <RouterLink class="btn-link btn-coral" to="/map">开始规划</RouterLink>
        <RouterLink class="btn-link btn-ghost" to="/cities">先找灵感</RouterLink>
      </div>
    </div>

    <div v-else class="journey-photo-feed" data-reveal style="--reveal-delay: 0.16s">
      <RouterLink
        v-for="item in items"
        :key="item.id"
        :to="`/trip/${item.id}`"
        class="journey-photo-card"
        :aria-label="`打开${item.title || item.destination_city || '旅行计划'}`"
      >
        <img v-if="tripCover(item)" :src="tripCover(item)" :alt="`${item.destination_city || ''}城市风景`" loading="lazy" />
        <span v-else class="journey-photo-fallback" aria-hidden="true">{{ item.destination_city || '下一站' }}</span>
        <span class="journey-photo-shade" aria-hidden="true" />
        <span class="badge journey-status" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
        <span class="journey-photo-copy">
          <span class="journey-duration">{{ item.travel_days ? `${item.travel_days} 天` : '旅行计划' }}</span>
          <strong>{{ item.destination_city || '目的地待定' }}</strong>
          <span class="journey-title">{{ item.title || '旅行计划' }}</span>
          <span class="journey-date">
            <template v-if="item.start_date || item.end_date">{{ item.start_date || '待定' }} — {{ item.end_date || '待定' }}</template>
            <template v-else>日期待定</template>
          </span>
          <b>打开这份行程 <ArrowRight :size="15" :stroke-width="2.2" /></b>
        </span>
      </RouterLink>
    </div>
    <div v-if="hasMore" class="load-more"><button type="button" class="btn-ghost" :disabled="loading" @click="load(limit + 20)">{{ loading ? '正在加载…' : `加载更多（还有 ${total - items.length} 份）` }}</button></div>

    <section class="chapter-bridge" data-reveal style="--reveal-delay: 0.24s">
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章</p>
        <h2 class="chapter-bridge-title">走过的路，写成回忆</h2>
        <p class="chapter-bridge-lead">把已经完成的行程整理成照片和笔记，留住路上的细节。</p>
      </div>
      <RouterLink class="chapter-bridge-cta" to="/memories">
        <span>去记录</span>
        <ArrowRight :size="18" :stroke-width="2.2" />
      </RouterLink>
    </section>
  </div>
</template>

<style scoped>
.journey-album-page {
  min-width: 0;
}

.journey-masthead {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 28px;
  padding: 30px 4px 28px;
  border-bottom: 1px solid var(--tm-line);
}

.journey-masthead p,
.journey-album-head > div > span,
.journey-duration {
  letter-spacing: .08em;
}

.journey-masthead p {
  margin: 0 0 10px;
  color: var(--tm-accent);
  font-size: 12px;
  font-weight: 800;
}

.journey-masthead h1 {
  margin: 0;
  color: var(--tm-ink);
  font-family: var(--font-display);
  font-size: clamp(34px, 5vw, 56px);
  line-height: 1.12;
  letter-spacing: -.04em;
}

.journey-masthead h1 em {
  color: var(--tm-accent);
  font-style: normal;
}

.journey-masthead > div > span {
  display: block;
  margin-top: 14px;
  color: var(--tm-muted);
}

.journey-masthead > .btn-link {
  flex-shrink: 0;
}

.journey-album-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  margin: 36px 0 18px;
}

.journey-album-head > div > span {
  color: var(--tm-muted);
  font-size: 11px;
  font-weight: 700;
}

.journey-album-head h2 {
  margin: 4px 0 0;
  color: var(--tm-ink);
  font-size: clamp(22px, 3vw, 30px);
}

.journey-photo-feed {
  column-count: 3;
  column-gap: 16px;
}

.journey-photo-card {
  position: relative;
  display: inline-flex;
  width: 100%;
  min-height: 440px;
  margin: 0 0 16px;
  overflow: hidden;
  break-inside: avoid;
  border-radius: 18px;
  background: var(--tm-paper);
  color: #fff;
  box-shadow: var(--tm-shadow-card);
  isolation: isolate;
  vertical-align: top;
}

/* Stable pseudo-random rhythm: card order alone controls its photo size. */
.journey-photo-card:nth-child(7n + 2),
.journey-photo-card:nth-child(7n + 6) { min-height: 340px; }
.journey-photo-card:nth-child(7n + 3) { min-height: 510px; }
.journey-photo-card:nth-child(7n + 5) { min-height: 380px; }

.journey-photo-card > img,
.journey-photo-fallback,
.journey-photo-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.journey-photo-card > img {
  object-fit: cover;
  transition: transform .55s cubic-bezier(.2, .8, .2, 1), filter .35s ease;
}

.journey-photo-fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, var(--tm-accent-deep), var(--tm-canvas-2));
  color: rgba(255, 255, 255, .44);
  font-family: var(--font-display);
  font-size: 34px;
}

.journey-photo-shade {
  z-index: 1;
  background: linear-gradient(180deg, rgba(12, 10, 8, .04) 16%, rgba(12, 10, 8, .28) 50%, rgba(12, 10, 8, .94) 100%);
}

.journey-status {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 2;
  border: 1px solid var(--tm-line-strong);
  background: var(--tm-paper-muted);
  backdrop-filter: blur(8px);
}

.journey-status.badge-ok { background: var(--tm-accent-soft); color: var(--tm-accent); }
.journey-status.badge-warn { background: var(--tm-sun-soft); color: var(--tm-sun); }
.journey-status.badge-muted { background: var(--tm-paper-raised); color: var(--tm-muted); }

.journey-photo-copy {
  position: relative;
  z-index: 2;
  align-self: flex-end;
  display: flex;
  width: 100%;
  flex-direction: column;
  padding: 70px 20px 20px;
}

.journey-duration {
  color: rgba(255, 255, 255, .72);
  font-size: 11px;
  font-weight: 700;
}

.journey-photo-copy > strong {
  margin-top: 6px;
  font-family: var(--font-display);
  font-size: clamp(30px, 3vw, 38px);
  line-height: 1.12;
  text-shadow: 0 2px 14px rgba(0, 0, 0, .28);
}

.journey-title {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 700;
}

.journey-date {
  margin-top: 4px;
  color: rgba(255, 255, 255, .74);
  font-size: 12px;
}

.journey-photo-copy > b {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--tm-line);
  font-size: 12px;
  text-align: right;
}

.journey-photo-card:hover > img {
  transform: scale(1.045);
  filter: saturate(1.08);
}

.journey-photo-card:focus-visible {
  outline: 3px solid var(--tm-accent);
  outline-offset: -3px;
}

@media (max-width: 900px) {
  .journey-masthead { align-items: flex-start; flex-direction: column; }
  .journey-photo-feed { column-count: 2; }
}

@media (max-width: 620px) {
  .journey-masthead { padding-top: 14px; }
  .journey-masthead h1 { font-size: 38px; }
  .journey-masthead h1 em { display: block; }
  .journey-masthead > .btn-link { width: 100%; justify-content: center; }
  .journey-photo-feed { column-count: 1; }
  .journey-photo-card,
  .journey-photo-card:nth-child(n) { min-height: 410px; }
  .journey-photo-card:nth-child(4n + 2) { min-height: 330px; }
  .journey-photo-copy { padding-inline: 17px; }
}

@media (prefers-reduced-motion: reduce) {
  .journey-photo-card > img { transition: none; }
}
</style>
