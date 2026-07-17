<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter, RouterLink } from 'vue-router';
import { tripApi } from '../api/trip.js';
import { aiApi } from '../api/ai.js';
import { memoryApi } from '../api/memory.js';
import TravelMap3D from '../components/map/AsyncTravelMap3D.vue';
import { currentTripDayIndex, tripCalendar } from '../utils/tripDeparture.js';

const route = useRoute();
const router = useRouter();
const detail = ref(null);
const error = ref('');
const chatText = ref('第二天会不会太赶？');
const replies = ref([]);
const comfort = ref(null);
const busy = ref('');
const departureMode = ref(false);
const checklist = ref({});
const expenses = ref({ budget: 0, actual: 0, remaining: 0, items: [] });
const expenseForm = reactive({ category: 'food', title: '', amount: '', spent_on: '' });
const expenseError = ref('');

const plan = computed(() => detail.value?.data || {});
const days = computed(() => plan.value.days || []);
const budget = computed(() => plan.value.budget || {});
const expenseItems = computed(() => expenses.value.items || []);
const expenseOverBudget = computed(() => Number(expenses.value.remaining || 0) < 0);
const departureDayIndex = computed(() => currentTripDayIndex(days.value));
const departureDay = computed(() => days.value[departureDayIndex.value] || null);
const departureStops = computed(() => {
  const day = departureDay.value;
  if (!day) return [];
  return [
    ...(day.attractions || []).map((item) => ({ ...item, type: '景点' })),
    ...(day.meals || []).map((item) => ({ ...item, type: '餐饮' })),
    ...(day.hotel?.name ? [{ ...day.hotel, type: '住宿' }] : []),
  ];
});
const departureChecks = computed(() => {
  const day = departureDay.value;
  if (!day) return [];
  return [
    { id: 'route', label: '确认当天出发时间与路线' },
    ...(day.hotel?.name ? [{ id: 'hotel', label: `确认入住：${day.hotel.name}` }] : []),
    ...(day.attractions?.length ? [{ id: 'tickets', label: '确认景点预约、门票与开放时间' }] : []),
    { id: 'power', label: '带好手机、充电宝和证件' },
  ];
});
const inspirationSources = computed(() => plan.value.inspiration_sources || []);
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
    try {
      checklist.value = JSON.parse(localStorage.getItem(`travel-mind-trip-checks-${route.params.id}`) || '{}');
    } catch {
      checklist.value = {};
    }
    const [comfortResult, expenseResult] = await Promise.allSettled([
      aiApi.tripComfort(route.params.id),
      tripApi.expenses(route.params.id),
    ]);
    if (comfortResult.status === 'fulfilled') comfort.value = comfortResult.value;
    if (expenseResult.status === 'fulfilled') expenses.value = expenseResult.value;
    else expenseError.value = '实际花费暂时不可用，稍后可以再试。';
  } catch (err) {
    error.value = err?.response?.data?.msg || err?.message || '打不开这趟行程';
  }
}

function mapLink(stop) {
  return `https://uri.amap.com/search?keyword=${encodeURIComponent([stop.name, stop.address || stop.city || plan.value.city].filter(Boolean).join(' '))}`;
}

function toggleCheck(id) {
  checklist.value = { ...checklist.value, [id]: !checklist.value[id] };
  try {
    localStorage.setItem(`travel-mind-trip-checks-${route.params.id}`, JSON.stringify(checklist.value));
  } catch {
    // 本机存储不可用时，当前页面仍可正常勾选。
  }
}

function exportCalendar() {
  const url = URL.createObjectURL(new Blob([tripCalendar(plan.value)], { type: 'text/calendar;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = `${plan.value.city || '旅行'}行程.ics`;
  link.click();
  URL.revokeObjectURL(url);
}

function replanWith(reply) {
  router.push({ path: '/planning', query: { city: plan.value.city, assistant: reply.slice(0, 500) } });
}

function money(value) {
  return Number(value || 0).toFixed(2);
}

async function addExpense() {
  busy.value = 'expense';
  expenseError.value = '';
  try {
    expenses.value = await tripApi.addExpense(route.params.id, { ...expenseForm });
    Object.assign(expenseForm, { category: 'food', title: '', amount: '', spent_on: '' });
  } catch (err) {
    expenseError.value = err?.message || '这笔花费没有记下来。';
  } finally {
    busy.value = '';
  }
}

async function removeExpense(expenseId) {
  if (!window.confirm('删掉这笔花费？')) return;
  busy.value = `expense-${expenseId}`;
  try {
    await tripApi.removeExpense(route.params.id, expenseId);
    expenses.value = await tripApi.expenses(route.params.id);
  } catch (err) {
    expenseError.value = err?.message || '删除花费失败。';
  } finally {
    busy.value = '';
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

async function createMemory() {
  busy.value = 'memory';
  error.value = '';
  try {
    const created = await memoryApi.createFromTrip(route.params.id);
    router.push(`/memories/${created.id}`);
  } catch (err) {
    error.value = err?.message || '旅行回忆没有生成，请稍后重试。';
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
    const response = await tripApi.chat(route.params.id, chatText.value, [...replies.value]);
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
      <button type="button" class="btn-coral" :disabled="busy === 'memory'" @click="createMemory">
        {{ busy === 'memory' ? '生成中…' : '生成旅行回忆' }}
      </button>
      <button type="button" class="btn-ghost" @click="departureMode = !departureMode">
        {{ departureMode ? '收起出发模式' : '进入出发模式' }}
      </button>
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

  <section v-if="days.length" class="trip-route-section" aria-labelledby="trip-route-title">
    <div class="section-head">
      <div>
        <p class="eyebrow">先看每天怎么走</p>
        <h2 id="trip-route-title">逐日路线</h2>
      </div>
    </div>

    <div class="route-timeline">
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
  </section>

  <div v-else-if="detail" class="empty-state empty-state--card" style="margin-top: 8px;">
    <strong>日程细节还没排出来</strong>
    <p>可以再生成一版，或直接在下方问这趟行程。</p>
    <div class="actions" style="justify-content: center; margin-top: 16px;">
      <RouterLink class="btn-link btn-coral" to="/planning">重新规划</RouterLink>
    </div>
  </div>

  <section v-if="detail && departureMode && departureDay" class="trip-departure glass-panel">
    <div class="trip-departure-head">
      <div>
        <p class="eyebrow">出发模式</p>
        <h2>Day {{ departureDayIndex + 1 }} · {{ departureDay.date || '今天的安排' }}</h2>
        <p>{{ departureDay.description || departureDay.city || plan.city }}</p>
      </div>
      <button type="button" class="btn-ghost" @click="exportCalendar">导入手机日历</button>
    </div>
    <div class="trip-departure-grid">
      <div>
        <h3>现在去哪</h3>
        <a v-for="stop in departureStops" :key="`${stop.type}-${stop.name}`" class="trip-departure-stop" :href="mapLink(stop)" target="_blank" rel="noreferrer">
          <span>{{ stop.type }}</span><strong>{{ stop.name }}</strong><small>{{ stop.address || departureDay.city || plan.city }} · 去导航 →</small>
        </a>
        <p v-if="!departureStops.length" class="trip-departure-empty">今天还没有具体停靠点，可以先问 AI 调整。</p>
      </div>
      <div>
        <h3>出门前确认</h3>
        <label v-for="item in departureChecks" :key="item.id" class="trip-departure-check" :class="{ 'is-done': checklist[item.id] }">
          <input type="checkbox" :checked="checklist[item.id]" @change="toggleCheck(item.id)" />
          <span>{{ item.label }}</span>
        </label>
      </div>
    </div>
  </section>

  <section v-if="detail && plan.city" class="glass-panel trip-map-panel">
    <div class="planner-map-head">
      <div>
        <p class="eyebrow">路线辅助</p>
        <h2>地图与导航</h2>
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

  <section v-if="detail" class="trip-summary">
    <article class="trip-summary-card">
      <span>预算大约</span>
      <strong>¥{{ budget.total || 0 }}</strong>
    </article>
    <article class="trip-summary-card">
      <span>走起来累不累</span>
      <strong>{{ comfortLabel }}</strong>
      <p>{{ riskLevelLabel }}</p>
    </article>
    <article class="trip-summary-card">
      <span>安排了多少</span>
      <strong>{{ stopCount }} 处停靠</strong>
      <p>{{ days.length || 0 }} 天</p>
    </article>
  </section>

  <section v-if="detail" class="trip-expense glass-panel">
    <div class="trip-expense-head">
      <div><p class="eyebrow">实际花费</p><h2>预算花到哪了</h2></div>
      <strong :class="{ 'is-over': expenseOverBudget }">剩余 ¥{{ money(expenses.remaining) }}</strong>
    </div>
    <div class="trip-expense-numbers">
      <span>计划预算 <b>¥{{ money(expenses.budget || budget.total) }}</b></span>
      <span>已经花了 <b>¥{{ money(expenses.actual) }}</b></span>
    </div>
    <p v-if="expenseError" class="error-line">{{ expenseError }}</p>
    <form class="trip-expense-form" @submit.prevent="addExpense">
      <select v-model="expenseForm.category" aria-label="花费分类"><option value="transport">交通</option><option value="stay">住宿</option><option value="food">餐饮</option><option value="ticket">门票</option><option value="shopping">购物</option><option value="other">其他</option></select>
      <input v-model.trim="expenseForm.title" required maxlength="128" placeholder="例如：西湖边午餐" />
      <input v-model="expenseForm.amount" required type="number" min="0.01" max="1000000" step="0.01" inputmode="decimal" placeholder="金额" />
      <input v-model="expenseForm.spent_on" type="date" aria-label="消费日期" />
      <button type="submit" class="btn-coral" :disabled="busy === 'expense'">{{ busy === 'expense' ? '记账中…' : '记一笔' }}</button>
    </form>
    <div v-if="expenseItems.length" class="trip-expense-list">
      <article v-for="item in expenseItems" :key="item.id"><span>{{ { transport: '交通', stay: '住宿', food: '餐饮', ticket: '门票', shopping: '购物', other: '其他' }[item.category] || '其他' }}</span><strong>{{ item.title }}</strong><time>{{ item.spent_on || '今天' }}</time><b>¥{{ money(item.amount) }}</b><button type="button" :disabled="busy === `expense-${item.id}`" @click="removeExpense(item.id)">删除</button></article>
    </div>
    <p v-else class="trip-expense-empty">还没有实际支出，先记下第一笔，预算才会开始帮你把关。</p>
  </section>

  <section v-if="inspirationSources.length" class="trip-source-section glass-panel">
    <p class="eyebrow">本次引用的社区分享</p>
    <h2>AI 参考了这些真实体验</h2>
    <div class="trip-source-list">
      <article v-for="source in inspirationSources" :key="source.post_id">
        <span>{{ source.intent === 'must' ? '必须安排' : source.intent === 'priority' ? '优先参考' : '体验参考' }}</span>
        <div><h3>{{ source.title }}</h3><p>{{ source.excerpt }}</p></div>
        <RouterLink :to="`/inspirations/${source.post_id}`">原帖 →</RouterLink>
      </article>
    </div>
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
        </div>
      </div>

      <div class="trip-suggestion-list">
        <h3>优先建议</h3>
        <p v-for="suggestion in comfortSuggestions" :key="suggestion">{{ suggestion }}</p>
      </div>
    </div>
  </section>

  <section v-if="detail" class="glass-panel trip-chat-panel">
    <h2>问问这趟行程</h2>
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
          <button type="button" class="text-action text-action--primary" @click="replanWith(item.reply)">按这条建议重新规划 →</button>
        </div>
      </template>
    </div>
  </section>
</template>
