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
  } catch (err) {
    // silent
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
    <PagePrologue index="09" eyebrow="智能推荐" title="为你挑选的下一站" lead="基于你的旅行偏好与收藏行为，为你推荐最适合的目的地与行程灵感。" next-label="探索目的地" next-to="/cities" />

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
      <div v-for="(item, index) in items" :key="`${item.itemType}-${item.itemId}`" class="rec-card">
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
            <button type="button" class="rec-action-btn" title="喜欢" @click="sendFeedback(item, 'like')">
              <ThumbsUp :size="14" />
            </button>
            <button type="button" class="rec-action-btn" title="不感兴趣" @click="sendFeedback(item, 'ignore')">
              <EyeOff :size="14" />
            </button>
            <RouterLink v-if="item.itemType === 'city'" class="rec-action-link" :to="{ path: '/map', query: { city: item.name } }">
              规划行程 <ArrowRight :size="12" />
            </RouterLink>
            <RouterLink v-else class="rec-action-link" :to="{ path: '/cities' }">
              查看详情 <ArrowRight :size="12" />
            </RouterLink>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recommendation-page {
  padding-bottom: 4rem;
}
.rec-filter-bar {
  display: flex;
  gap: 0.5rem;
  padding: 0 1.25rem;
  margin: 0.75rem 0 1.25rem;
  flex-wrap: wrap;
}
.rec-chip {
  appearance: none;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
}
.rec-chip.active,
.rec-chip:hover {
  background: #0ea5e9;
  color: #fff;
  border-color: #0ea5e9;
}
.rec-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 1rem;
  padding: 0 1.25rem;
}
.rec-card {
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 14px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.rec-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.08);
}
.rec-card-cover {
  position: relative;
  height: 150px;
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0ea5e9;
}
.rec-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.rec-card-cover em {
  position: absolute;
  top: 0.6rem;
  left: 0.6rem;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 0.7rem;
  padding: 0.2rem 0.5rem;
  border-radius: 999px;
  font-style: normal;
}
.rec-card-body {
  padding: 0.9rem 1rem 1rem;
}
.rec-card-kicker {
  font-size: 0.75rem;
  color: #64748b;
  margin: 0 0 0.25rem;
}
.rec-card-body h3 {
  font-size: 1rem;
  margin: 0 0 0.4rem;
  color: #0f172a;
}
.rec-card-body > p {
  font-size: 0.8rem;
  color: #475569;
  margin: 0 0 0.6rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.rec-card-meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.6rem;
}
.rec-stars {
  display: inline-flex;
  align-items: center;
  gap: 0.15rem;
  color: #cbd5e1;
}
.rec-stars .filled {
  color: #f59e0b;
}
.rec-stars small {
  margin-left: 0.3rem;
  font-size: 0.75rem;
  color: #64748b;
}
.rec-popularity {
  font-size: 0.75rem;
  color: #64748b;
}
.rec-card-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.rec-action-btn {
  appearance: none;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}
.rec-action-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}
.rec-action-link {
  margin-left: auto;
  font-size: 0.8rem;
  color: #0ea5e9;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
}
.rec-action-link:hover {
  text-decoration: underline;
}
</style>
