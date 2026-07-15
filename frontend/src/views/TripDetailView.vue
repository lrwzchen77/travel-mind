<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter, RouterLink } from 'vue-router';
import { tripApi } from '../api/trip.js';
import { aiApi } from '../api/ai.js';
import TravelMap3D from '../components/map/AsyncTravelMap3D.vue';

const route = useRoute();
const router = useRouter();
const detail = ref(null);
const error = ref('');
const chatText = ref('这趟预算大概多少？');
const replies = ref([]);
const comfort = ref(null);
const busy = ref('');

const plan = computed(() => detail.value?.data || {});
const days = computed(() => plan.value.days || []);
const budget = computed(() => plan.value.budget || {});
const comfortJson = computed(() => {
  const value = comfort.value?.result_json;
  if (!value) return {};
  if (typeof value === 'object') return value;
  try {
    return JSON.parse(value);
  } catch {
    return {};
  }
});
const comfortData = computed(() => comfortJson.value.data || comfortJson.value);

async function load() {
  error.value = '';
  try {
    detail.value = await tripApi.detail(route.params.id);
    comfort.value = await aiApi.tripComfort(route.params.id);
  } catch (err) {
    error.value = err?.response?.data?.msg || err?.message || '打不开这趟行程';
  }
}

async function copyPlan() {
  busy.value = 'copy';
  try {
    const copied = await tripApi.copy(route.params.id);
    router.push(`/trip/${copied.plan_id}`);
  } catch (err) {
    error.value = err?.message || '复制失败';
  } finally {
    busy.value = '';
  }
}

async function deletePlan() {
  if (!window.confirm('确定丢掉这趟行程吗？删了就找不回来了。')) return;
  busy.value = 'delete';
  try {
    await tripApi.remove(route.params.id);
    router.push('/trip-history');
  } catch (err) {
    error.value = err?.message || '删除失败';
  } finally {
    busy.value = '';
  }
}

async function chat() {
  if (!chatText.value.trim()) return;
  busy.value = 'chat';
  try {
    const response = await tripApi.chat(route.params.id, chatText.value, replies.value);
    replies.value.push({ message: chatText.value, reply: response.reply });
    chatText.value = '';
  } catch (err) {
    error.value = err?.message || '对话失败';
  } finally {
    busy.value = '';
  }
}

onMounted(load);
</script>

<template>
  <section class="home-hero" style="min-height: 220px; margin-bottom: 28px; padding-bottom: 32px;">
    <p class="hero-kicker">行程 #{{ route.params.id }}</p>
    <h1 style="font-size: clamp(28px, 4vw, 40px);">{{ plan.city || '行程详情' }}</h1>
    <p v-if="plan.start_date" class="lead">
      {{ plan.start_date }} — {{ plan.end_date }}
      <template v-if="plan.overall_suggestions"> · {{ plan.overall_suggestions }}</template>
    </p>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <section v-if="detail" class="metric-row">
    <div class="metric-tile">
      <div class="label">预算大约</div>
      <div class="value">¥{{ budget.total || 0 }}</div>
      <p>含行程估算，以实际为准</p>
    </div>
    <div class="metric-tile">
      <div class="label">舒适度</div>
      <div class="value">{{ comfortData.comfort_score ?? '—' }}</div>
      <p>{{ comfortData.risk_level || comfort?.status || '还没有评估结果' }}</p>
    </div>
    <div class="metric-tile">
      <div class="label">路线节点</div>
      <div class="value">{{ detail.graph_data?.nodes?.length || days.length || 0 }}</div>
      <p>{{ detail.graph_data?.edges?.length || 0 }} 段关联</p>
    </div>
  </section>

  <div v-if="detail" class="actions" style="margin-bottom: 24px;">
    <button type="button" class="btn-ghost" :disabled="busy === 'copy'" @click="copyPlan">
      {{ busy === 'copy' ? '复制中…' : '复制一程' }}
    </button>
    <button type="button" class="btn-danger" :disabled="busy === 'delete'" @click="deletePlan">
      {{ busy === 'delete' ? '删除中…' : '删除行程' }}
    </button>
    <button type="button" class="btn-ghost" @click="router.push('/trip-history')">返回我的行程</button>
    <RouterLink
      v-if="plan.city"
      class="btn-link btn-ghost"
      style="min-height: 44px;"
      :to="{ path: '/map', query: { city: plan.city } }"
    >在立体地图中查看</RouterLink>
  </div>

  <section v-if="detail && plan.city" class="glass-panel" style="margin-bottom: 24px; padding-bottom: 16px;">
    <div class="planner-map-head">
      <div>
        <h2 style="margin: 0 0 6px; font-family: var(--font-display); font-size: 20px;">目的地三维视野</h2>
        <p class="panel-hint" style="margin: 0;">{{ plan.city }} · 旋转看看这座城的天际线</p>
      </div>
    </div>
    <TravelMap3D
      :city="plan.city"
      height="340px"
      compact
      :show-pois="true"
      style="margin-top: 12px;"
    />
  </section>

  <div class="section-head" v-if="days.length">
    <div>
      <h2>逐日路线</h2>
      <p>像翻攻略一样往下看</p>
    </div>
  </div>

  <div v-if="days.length" class="route-timeline">
    <article v-for="(day, idx) in days" :key="day.day_index ?? idx" class="route-day">
      <div class="route-axis"><span class="route-node" /></div>
      <div class="route-card">
        <h3>Day {{ day.day_index || idx + 1 }} · {{ day.date }}</h3>
        <p class="day-meta">
          {{ day.description || day.city || '今天的安排' }}
          <template v-if="day.transportation"> · {{ day.transportation }}</template>
        </p>
        <div class="route-chips">
          <span
            v-for="item in (day.attractions || [])"
            :key="`a-${item.name}`"
            class="chip chip-accent"
          >景 · {{ item.name }}</span>
          <span v-for="item in (day.meals || [])" :key="`m-${item.name}`" class="chip">食 · {{ item.name }}</span>
          <span v-if="day.hotel?.name" class="chip">住 · {{ day.hotel.name }}</span>
        </div>
      </div>
    </article>
  </div>

  <section v-if="detail" class="glass-panel" style="margin-top: 28px; max-width: 640px;">
    <h2>问问这趟行程</h2>
    <p class="panel-hint">预算紧不紧、某天累不累、要不要改动——直接问。</p>
    <textarea v-model="chatText" rows="3" spellcheck="false" placeholder="例如：第二天会不会太赶？" />
    <div class="actions" style="margin-top: 12px;">
      <button type="button" class="btn-coral" :disabled="busy === 'chat'" @click="chat">
        {{ busy === 'chat' ? '发送中…' : '发送' }}
      </button>
    </div>
    <div class="chat-list">
      <template v-for="(item, index) in replies" :key="`${index}-${item.message}`">
        <div class="chat-bubble is-user">
          <h3>你</h3>
          <p>{{ item.message }}</p>
        </div>
        <div class="chat-bubble">
          <h3>Travel Mind</h3>
          <p>{{ item.reply }}</p>
        </div>
      </template>
    </div>
  </section>
</template>
