<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ArrowRight, Star, MapPin, ThumbsUp, EyeOff } from 'lucide-vue-next';
import { recommendationApi } from '../api/recommendation.js';
import PagePrologue from '../components/PagePrologue.vue';
import { useReveal } from '../composables/useReveal.js';
import { cityImageByName } from '../data/cityImages.js';

const root = ref(null);
useReveal(root);

const items = ref([]);
const loading = ref(false);
const error = ref('');
const currentType = ref('city');
const currentCity = ref('');

const typeLabel = computed(() => {
  const map = { city: '城市', attraction: '景点', hotel: '住宿', restaurant: '餐厅' };
  return map[currentType.value] || '目的地';
});

async function load(type = currentType.value, city = currentCity.value) {
  loading.value = true;
  error.value = '';
  currentType.value = type;
  currentCity.value = city;
  try {
    const data = await recommendationApi.list(type, city || undefined, 12);
    items.value = data || [];
  } catch (err) {
    error.value = err?.message || '推荐服务暂时不可用，请稍后重试。';
  } finally {
    loading.value = false;
  }
}

async function sendFeedback(item, feedback) {
  try {
    await recommendationApi.feedback(item.itemId, item.itemType, feedback);
    item.feedback = feedback;
  } catch (err) {
    error.value = err?.message || '反馈提交失败，请稍后重试。';
  }
}

function coverUrl(item) {
  return cityImageByName[item.name] || cityImageByName[item.city] || '';
}

function scoreStars(rating) {
  const r = Math.round((rating || 0) / 5 * 5);
  return r;
}

onMounted(() => load('city'));
</script>

<template>
  <div ref="root" class="recommendation-page">
    <PagePrologue index="04 · 推荐" eyebrow="智能推荐" title="为你挑选的下一站" lead="基于你的旅行偏好与收藏行为，为你推荐最适合的目的地与行程灵感。" next-label="探索目的地" next-to="/cities" />

    <p v-if="error" class="error-line" role="alert">{{ error }}</p>

    <div class="section-head" data-reveal style="--reveal-delay: 0s">
      <div>
        <h2>{{ loading ? '正在为你推荐…' : `${typeLabel}推荐` }}</h2>
      </div>
    </div>

    <div class="rec-filter-bar" data-reveal style="--reveal-delay: 0.04s">
      <button type="button" class="rec-chip" :class="{ active: currentType === 'city' }" @click="load('city')">城市</button>
      <button type="button" class="rec-chip" :class="{ active: currentType === 'attraction' }" @click="load('attraction')">景点</button>
      <button type="button" class="rec-chip" :class="{ active: currentType === 'hotel' }" @click="load('hotel')">住宿</button>
      <button type="button" class="rec-chip" :class="{ active: currentType === 'restaurant' }" @click="load('restaurant')">餐厅</button>
    </div>

    <div v-if="!loading && !items.length" class="empty-state empty-state--card" data-reveal style="--reveal-delay: 0.08s">
      <strong>暂无推荐</strong>
      <p>完善你的旅行偏好，我们将为你提供更精准的推荐。</p>
      <RouterLink class="btn-link btn-coral" to="/profile">设置旅行偏好</RouterLink>
    </div>

    <div v-else class="rec-grid" data-reveal style="--reveal-delay: 0.08s" aria-live="polite">
      <article v-for="item in items" :key="`${item.itemType}-${item.itemId}`" class="rec-card">
        <div class="rec-card-cover" :class="{ 'has-photo': coverUrl(item) }">
          <img v-if="coverUrl(item)" :src="coverUrl(item)" :alt="item.name" loading="lazy" />
          <span v-else aria-hidden="true"><MapPin :size="28" /></span>
          <em v-if="item.matchReason">{{ item.matchReason }}</em>
        </div>
        <div class="rec-card-body">
          <p class="rec-card-kicker">{{ item.city || '目的地' }} · {{ item.itemType === 'city' ? '城市' : (item.itemType === 'attraction' ? '景点' : (item.itemType === 'hotel' ? '住宿' : '餐厅')) }}</p>
          <h3>{{ item.name }}</h3>
          <p>{{ item.description || '暂无描述' }}</p>
          <div class="rec-card-meta">
            <span class="rec-stars" aria-label="评分">
              <Star v-for="s in 5" :key="s" :size="12" :class="{ filled: s <= scoreStars(item.rating) }" />
              <small>{{ item.rating ? item.rating.toFixed(1) : '暂无评分' }}</small>
            </span>
            <span v-if="item.popularity" class="rec-popularity">热度 {{ item.popularity }}</span>
          </div>
          <div class="rec-card-actions">
            <button type="button" class="rec-action-btn" :class="{ active: item.feedback === 'like' }" :disabled="Boolean(item.feedback)" aria-label="喜欢这条推荐" @click="sendFeedback(item, 'like')">
              <ThumbsUp :size="14" />
            </button>
            <button type="button" class="rec-action-btn" :class="{ active: item.feedback === 'ignore' }" :disabled="Boolean(item.feedback)" aria-label="对这条推荐不感兴趣" @click="sendFeedback(item, 'ignore')">
              <EyeOff :size="14" />
            </button>
            <RouterLink v-if="item.itemType === 'city'" class="rec-action-link" :to="{ path: '/map', query: { city: item.name } }">
              规划行程 <ArrowRight :size="12" />
            </RouterLink>
            <RouterLink v-else class="rec-action-link" :to="`/discover/${item.itemType}s/${item.itemId}`">
              查看详情 <ArrowRight :size="12" />
            </RouterLink>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.recommendation-page {
  padding-bottom: 64px;
}
.rec-filter-bar {
  display: flex;
  gap: 8px;
  margin: 12px 0 24px;
  flex-wrap: wrap;
}
.rec-chip {
  appearance: none;
  min-height: 40px;
  border: 1px solid var(--tm-line-strong);
  background: var(--tm-paper-muted);
  color: var(--tm-ink-soft);
  padding: 0 16px;
  border-radius: var(--tm-radius-pill);
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.25s ease, background 0.25s ease, color 0.25s ease;
}
.rec-chip.active,
.rec-chip:hover {
  background: var(--tm-accent-soft);
  color: var(--tm-accent);
  border-color: var(--tm-accent);
}
.rec-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
}
.rec-card {
  background: var(--tm-paper);
  border: 1px solid var(--tm-line);
  border-radius: var(--tm-radius-panel);
  overflow: hidden;
  box-shadow: var(--tm-shadow-card);
  transition: transform 0.4s var(--ease-out-expo), box-shadow 0.4s ease, border-color 0.4s ease;
}
.rec-card:hover {
  transform: translateY(-6px);
  border-color: var(--tm-accent);
  box-shadow: var(--tm-shadow-lift);
}
.rec-card-cover {
  position: relative;
  height: 180px;
  background: linear-gradient(145deg, var(--tm-accent-deep), var(--tm-canvas-2));
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--tm-accent);
}
.rec-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.rec-card-cover em {
  position: absolute;
  top: 12px;
  left: 12px;
  max-width: calc(100% - 24px);
  padding: 5px 9px;
  border: 1px solid var(--tm-line-strong);
  border-radius: var(--tm-radius-pill);
  background: rgba(12, 10, 8, 0.68);
  color: var(--tm-ink);
  font: 500 11px/1.4 var(--font-mono);
  font-style: normal;
  backdrop-filter: blur(8px);
}
.rec-card-body {
  padding: 18px;
}
.rec-card-kicker {
  margin: 0 0 6px;
  color: var(--tm-accent);
  font: 600 11px/1.4 var(--font-mono);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.rec-card-body h3 {
  margin: 0 0 8px;
  color: var(--tm-ink);
  font-size: 20px;
}
.rec-card-body > p {
  min-height: 44px;
  margin: 0 0 12px;
  color: var(--tm-muted);
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.rec-card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.rec-stars {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--tm-line-strong);
}
.rec-stars .filled {
  color: var(--tm-sun);
}
.rec-stars small {
  margin-left: 5px;
  color: var(--tm-muted);
  font-size: 12px;
}
.rec-popularity {
  color: var(--tm-muted);
  font-size: 12px;
}
.rec-card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--tm-line);
}
.rec-action-btn {
  appearance: none;
  width: 40px;
  height: 40px;
  border: 1px solid var(--tm-line-strong);
  border-radius: var(--tm-radius-control);
  background: var(--tm-paper-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--tm-muted);
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}
.rec-action-btn:hover {
  border-color: var(--tm-accent);
  background: var(--tm-accent-soft);
  color: var(--tm-accent);
}
.rec-action-btn.active { border-color: var(--tm-accent); color: var(--tm-accent); }
.rec-action-btn:disabled { cursor: default; opacity: 0.72; }
.rec-action-link {
  margin-left: auto;
  color: var(--tm-accent);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.rec-action-link:hover {
  color: var(--tm-accent-hover);
}
</style>
