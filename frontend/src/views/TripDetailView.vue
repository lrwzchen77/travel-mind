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
const chatText = ref('第二天会不会太赶？');
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
const comfortSuggestions = computed(() => comfortData.value.suggestions || []);
const dailyRisks = computed(() => (comfortData.value.daily_risks || []).filter((day) => day.risk_items?.length));
const riskLevelLabel = computed(() => ({
  low: '整体轻松',
  medium: '需要留意',
  high: '节奏偏紧',
}[comfortData.value.risk_level] || '已完成检查'));

const comfortLabel = computed(() => {
  const score = comfortData.value.comfort_score;
  if (score == null || score === '') return '待评估';
  const n = Number(score);
  if (Number.isNaN(n)) return String(score);
  if (n >= 80) return `${n} · 轻松`;
  if (n >= 60) return `${n} · 适中`;
  return `${n} · 偏紧`;
});

const stopCount = computed(() => {
  let n = 0;
  for (const day of days.value) {
    n += (day.attractions || []).length;
    n += (day.meals || []).length;
    if (day.hotel?.name) n += 1;
  }
  if (n > 0) return n;
  return detail.value?.graph_data?.nodes?.length || days.value.length || 0;
});

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
    error.value = err?.message || '这次没问成，稍后再试';
  } finally {
    busy.value = '';
  }
}

async function askAbout(question) {
  if (busy.value === 'chat') return;
  chatText.value = question;
  await chat();
}

onMounted(load);
</script>

<template>
  <section class="trip-hero">
    <div class="trip-hero-copy">
      <p class="eyebrow">我的旅行计划</p>
      <h1>{{ plan.city || '行程详情' }}</h1>
      <p v-if="plan.start_date || plan.end_date" class="lead">
        {{ plan.start_date || '待定' }} — {{ plan.end_date || '待定' }}
        <template v-if="plan.overall_suggestions"> · {{ plan.overall_suggestions }}</template>
      </p>
      <p v-else-if="!detail" class="lead">正在打开这趟行程…</p>
    </div>
    <div v-if="detail" class="trip-hero-actions">
      <button type="button" class="btn-ghost" :disabled="busy === 'copy'" @click="copyPlan">
        {{ busy === 'copy' ? '复制中…' : '复制一程' }}
      </button>
      <button type="button" class="btn-danger" :disabled="busy === 'delete'" @click="deletePlan">
        {{ busy === 'delete' ? '删除中…' : '丢掉这程' }}
      </button>
      <RouterLink class="btn-link btn-ghost" to="/trip-history">返回行程册</RouterLink>
    </div>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <section v-if="detail" class="trip-summary">
    <article class="trip-summary-card">
      <span>预算大约</span>
      <strong>¥{{ budget.total || 0 }}</strong>
      <p>估算仅供参考，以实际消费为准</p>
    </article>
    <article class="trip-summary-card">
      <span>走起来累不累</span>
      <strong>{{ comfortLabel }}</strong>
      <p>{{ riskLevelLabel }}</p>
    </article>
    <article class="trip-summary-card">
      <span>安排了多少</span>
      <strong>{{ stopCount }} 处停靠</strong>
      <p>{{ days.length || 0 }} 天 · 像翻攻略一样往下看</p>
    </article>
  </section>

  <section
    v-if="comfortData.comfort_score != null"
    class="trip-check-section"
    aria-labelledby="trip-check-title"
  >
    <div class="trip-check-head">
      <div>
        <p class="eyebrow">出发前检查</p>
        <h2 id="trip-check-title">哪些地方值得提前调整</h2>
        <p>根据每天的安排密度、城市换乘和天气条件检查。</p>
      </div>
      <span class="trip-check-score">{{ comfortLabel }}</span>
    </div>

    <div class="trip-check-layout">
      <div class="trip-risk-days">
        <article v-for="day in dailyRisks" :key="day.day_index">
          <span>Day {{ day.day_index }}</span>
          <div>
            <strong>{{ day.city || day.date || '当天安排' }}</strong>
            <p v-for="risk in day.risk_items" :key="risk">{{ risk }}</p>
          </div>
          <button
            type="button"
            class="text-action text-action--primary"
            :disabled="busy === 'chat'"
            @click="askAbout(`第${day.day_index}天有这些问题：${day.risk_items.join('；')}，请给我具体调整建议。`)"
          >问怎么调整 →</button>
        </article>
        <div v-if="!dailyRisks.length" class="trip-check-clear">
          <strong>每天的节奏都比较稳妥</strong>
          <p>继续保留交通和用餐缓冲即可。</p>
        </div>
      </div>

      <div class="trip-suggestion-list">
        <h3>优先建议</h3>
        <p v-for="suggestion in comfortSuggestions" :key="suggestion">{{ suggestion }}</p>
      </div>
    </div>
  </section>

  <section v-if="detail && plan.city" class="glass-panel trip-map-panel">
    <div class="planner-map-head">
      <div>
        <h2>目的地三维视野</h2>
        <p class="panel-hint" style="margin: 0;">{{ plan.city }} · 旋转看看这座城的天际线</p>
      </div>
      <RouterLink
        class="text-link"
        :to="{ path: '/map', query: { city: plan.city } }"
      >
        全屏地图 →
      </RouterLink>
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
      <p>一天一张故事卡</p>
    </div>
  </div>

  <div v-if="days.length" class="route-timeline">
    <article v-for="(day, idx) in days" :key="day.day_index ?? idx" class="route-day">
      <div class="route-axis"><span class="route-node" /></div>
      <div class="route-card">
        <h3>Day {{ day.day_index || idx + 1 }} · {{ day.date || '日期待定' }}</h3>
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
          <span
            v-for="item in (day.meals || [])"
            :key="`m-${item.name}`"
            class="chip"
          >食 · {{ item.name }}</span>
          <span v-if="day.hotel?.name" class="chip">住 · {{ day.hotel.name }}</span>
        </div>
      </div>
    </article>
  </div>

  <div v-else-if="detail" class="empty-state empty-state--card" style="margin-top: 8px;">
    <strong>日程细节还没排出来</strong>
    <p>可以再生成一版，或直接在下方问这趟行程。</p>
    <div class="actions" style="justify-content: center; margin-top: 16px;">
      <RouterLink class="btn-link btn-coral" to="/planning">重新规划</RouterLink>
    </div>
  </div>

  <section v-if="detail" class="glass-panel trip-chat-panel">
    <h2>问问这趟行程</h2>
    <p class="panel-hint">预算紧不紧、某天累不累、要不要改动——像问朋友一样直接说。</p>
    <div class="trip-quick-questions">
      <button type="button" :disabled="busy === 'chat'" @click="askAbout('哪一天最赶？请按优先级告诉我怎么减少景点。')">哪天最赶</button>
      <button type="button" :disabled="busy === 'chat'" @click="askAbout('想少走路，请帮我调整每天的游玩顺序。')">少走一点</button>
      <button type="button" :disabled="busy === 'chat'" @click="askAbout('预算还能怎么省？不要明显降低住宿和用餐体验。')">控制预算</button>
    </div>
    <textarea
      v-model="chatText"
      rows="3"
      spellcheck="false"
      placeholder="例如：想少走路，第二天怎么改？"
    />
    <div class="actions" style="margin-top: 12px;">
      <button type="button" class="btn-coral" :disabled="busy === 'chat'" @click="chat">
        {{ busy === 'chat' ? '思考中…' : '发送' }}
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
