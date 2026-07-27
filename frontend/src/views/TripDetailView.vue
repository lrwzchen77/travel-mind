<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter, RouterLink } from 'vue-router';
import { tripApi } from '../api/trip.js';
import { aiApi } from '../api/ai.js';
import { memoryApi } from '../api/memory.js';
import { journalApi } from '../api/journal.js';
import TravelMap3D from '../components/map/AsyncTravelMap3D.vue';
import PublicTravelDataPanel from '../components/PublicTravelDataPanel.vue';
import { consumerText } from '../data/consumerText.js';
import { currentTripDayIndex, tripCalendar } from '../utils/tripDeparture.js';
import { normalizeRouteIntent, ROUTE_INTENT_KEY } from '../map/trackEditor.js';
import { ArrowRight, BrainCircuit, CircleCheck, Coffee, Gauge, Zap } from 'lucide-vue-next';
import { useReveal } from '../composables/useReveal.js';

const root = ref(null);
useReveal(root);

const route = useRoute();
const router = useRouter();
const detail = ref(null);
const error = ref('');
const chatText = ref('第二天会不会太赶？');
const replies = ref([]);
const comfort = ref(null);
const comfortFeedback = ref({ submitted: false });
const feedbackError = ref('');
const busy = ref('');
const departureMode = ref(false);
const checklist = ref({});
const expenses = ref({ budget: 0, actual: 0, remaining: 0, items: [] });
const expenseForm = reactive({ category: 'food', title: '', amount: '', spent_on: '' });
const expenseError = ref('');
const mapRef = ref(null);
const editDraft = ref(null);
const poiPhotos = ref({});

const plan = computed(() => detail.value?.data || {});
const days = computed(() => plan.value.days || []);
const editDays = computed(() => editDraft.value?.data?.days || []);
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
const routeIntent = computed(() => normalizeRouteIntent(plan.value.route_intent, plan.value.city));
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
const comfortClasses = {
  relaxed: { label: '偏松', icon: Coffee },
  balanced: { label: '正合适', icon: CircleCheck },
  intense: { label: '太赶', icon: Zap },
};
const predictedComfort = computed(() => comfortClasses[comfortData.value.comfort_class] || { label: '待判断', icon: Gauge });
const comfortProbabilities = computed(() => ['relaxed', 'balanced', 'intense'].map((key) => ({
  key,
  ...comfortClasses[key],
  value: Number(comfortData.value.probabilities?.[key] || 0),
})));
const modelVersionLabel = computed(() => {
  const version = String(comfortData.value.model_version || '').match(/v\d+$/i)?.[0] || 'v1';
  return comfortData.value.model_mode === 'trained_travel_comfort' ? `TravelComfort ${version}` : '规则评估';
});
const tripEnded = computed(() => Boolean(plan.value.end_date) && plan.value.end_date <= localDate());
const feedbackAllowed = computed(() => tripEnded.value);
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

function localDate() {
  const now = new Date();
  const pad = (value) => String(value).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

function percent(value) {
  return `${Math.round(Number(value || 0) * 1000) / 10}%`;
}

async function load() {
  error.value = '';
  try {
    detail.value = await tripApi.detail(route.params.id);
    void loadPoiPhotos();
    try {
      checklist.value = JSON.parse(localStorage.getItem(`travel-mind-trip-checks-${route.params.id}`) || '{}');
    } catch {
      checklist.value = {};
    }
    const [comfortResult, feedbackResult, expenseResult] = await Promise.allSettled([
      aiApi.tripComfort(route.params.id),
      aiApi.tripComfortFeedback(route.params.id),
      tripApi.expenses(route.params.id),
    ]);
    if (comfortResult.status === 'fulfilled') comfort.value = comfortResult.value;
    if (feedbackResult.status === 'fulfilled') comfortFeedback.value = feedbackResult.value;
    else feedbackError.value = '暂时无法读取体验反馈。';
    if (expenseResult.status === 'fulfilled') expenses.value = expenseResult.value;
    else expenseError.value = '实际花费暂时不可用，稍后可以再试。';
  } catch (err) {
    error.value = err?.response?.data?.msg || err?.message || '打不开这趟行程';
  }
}

function poiPhotoKey(day, attraction) {
  return `${day.city || plan.value.city || ''}:${attraction?.name || ''}`;
}

function dayPhoto(day) {
  return poiPhotos.value[poiPhotoKey(day, day.attractions?.[0])] || '';
}

async function loadPoiPhotos() {
  // ponytail: one cover per day avoids an XHS request per attraction; expand only if the UI adds attraction cards.
  const targets = days.value
    .filter((day) => day.attractions?.[0]?.name)
    .map((day) => ({ day, attraction: day.attractions[0] }))
    .filter(({ day, attraction }, index, all) => all.findIndex((item) => poiPhotoKey(item.day, item.attraction) === poiPhotoKey(day, attraction)) === index)
    .slice(0, 8);
  const results = await Promise.allSettled(targets.map(({ day, attraction }) =>
    tripApi.poiPhoto(attraction.name, day.city || plan.value.city)));
  poiPhotos.value = Object.fromEntries(results.flatMap((result, index) => {
    const url = result.status === 'fulfilled' ? result.value?.data?.photo_url : '';
    return url ? [[poiPhotoKey(targets[index].day, targets[index].attraction), url]] : [];
  }));
}

async function saveComfortFeedback(actualLabel) {
  if (!feedbackAllowed.value) return;
  busy.value = 'comfort-feedback';
  feedbackError.value = '';
  try {
    comfortFeedback.value = await aiApi.saveTripComfortFeedback(route.params.id, { actual_label: actualLabel });
  } catch (err) {
    feedbackError.value = err?.response?.data?.msg || err?.message || '这次反馈没有保存成功。';
  } finally {
    busy.value = '';
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
  const query = { city: plan.value.city, assistant: reply.slice(0, 500) };
  if (routeIntent.value) {
    try {
      window.sessionStorage.setItem(ROUTE_INTENT_KEY, JSON.stringify(routeIntent.value));
      query.route = '1';
    } catch {
      // 存储不可用时仍允许按 AI 建议重新规划。
    }
  }
  router.push({ path: '/map', query });
}

function focusRouteNode(node) {
  mapRef.value?.flyToPoint?.(node);
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

function startEdit() {
  editDraft.value = JSON.parse(JSON.stringify(detail.value));
}

function addAttraction(day) {
  if (!Array.isArray(day.attractions)) day.attractions = [];
  day.attractions.push({ name: '', address: '', description: '', ticket_price: 0 });
}

async function savePlan() {
  busy.value = 'edit';
  error.value = '';
  try {
    detail.value = await tripApi.update(route.params.id, editDraft.value);
    editDraft.value = null;
  } catch (err) {
    error.value = err?.message || '行程保存失败';
  } finally {
    busy.value = '';
  }
}

async function createMemory() {
  busy.value = 'memory';
  error.value = '';
  try {
    const created = await memoryApi.createFromTrip(route.params.id);
    router.push({ path: `/memories/${created.id}`, query: { update: '1' } });
  } catch (err) {
    error.value = err?.message || '这趟旅行暂时没记录下来，请稍后重试。';
  } finally {
    busy.value = '';
  }
}

async function createJournal() {
  busy.value = 'journal';
  error.value = '';
  try {
    const created = await journalApi.createFromTrip(route.params.id);
    router.push(`/journals/${created.journal_id}`);
  } catch (err) {
    error.value = err?.message || '生成游记失败，请稍后重试。';
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
  <div ref="root">
  <section class="trip-hero" data-reveal>
    <div class="trip-hero-copy">
      <p class="eyebrow">我的旅行计划</p>
      <h1>{{ plan.city || '行程详情' }}</h1>
      <p v-if="plan.start_date || plan.end_date" class="lead">
        {{ plan.start_date || '待定' }} — {{ plan.end_date || '待定' }}
        <template v-if="plan.overall_suggestions"> · {{ consumerText(plan.overall_suggestions) }}</template>
      </p>
      <p v-else-if="!detail" class="lead">正在打开这趟行程…</p>
    </div>
    <div v-if="detail" class="trip-hero-actions">
      <button type="button" class="btn-coral" :disabled="busy === 'memory' || !tripEnded" @click="createMemory">
        {{ busy === 'memory' ? '正在记录…' : (tripEnded ? '记录这趟旅行' : '行程结束后可记录') }}
      </button>
      <button type="button" class="btn-coral" :disabled="busy === 'journal'" @click="createJournal">
        {{ busy === 'journal' ? '生成中…' : '生成游记' }}
      </button>
      <button type="button" class="btn-ghost" @click="departureMode = !departureMode">
        {{ departureMode ? '收起出发模式' : '进入出发模式' }}
      </button>
      <button type="button" class="btn-ghost" :disabled="busy === 'copy'" @click="copyPlan">
        {{ busy === 'copy' ? '复制中…' : '复制一程' }}
      </button>
      <button v-if="!editDraft" type="button" class="btn-ghost" @click="startEdit">编辑行程</button>
      <button type="button" class="btn-danger" :disabled="busy === 'delete'" @click="deletePlan">
        {{ busy === 'delete' ? '删除中…' : '丢掉这程' }}
      </button>
      <RouterLink class="btn-link btn-ghost" to="/trip-history">返回行程册</RouterLink>
    </div>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>
  <p v-if="detail" class="trust-note">AI 推荐 · 未预订。行程中的地点、价格、库存、营业时间和预约要求可能变动，出发前请向服务方复核。</p>

  <PublicTravelDataPanel v-if="plan.public_data?.length" :items="plan.public_data" />

  <form v-if="editDraft" class="glass-panel field-stack" @submit.prevent="savePlan">
    <div class="section-head"><div><p class="eyebrow">手动调整</p><h2>编辑这趟行程</h2></div></div>
    <label><span class="field-label">整体建议</span><textarea v-model="editDraft.data.overall_suggestions" maxlength="1000" rows="3" /></label>
    <article v-for="(day, dayIndex) in editDays" :key="dayIndex" class="route-card field-stack">
      <h3>Day {{ dayIndex + 1 }} · {{ day.date || '日期待定' }}</h3>
      <label><span class="field-label">当天说明</span><textarea v-model="day.description" maxlength="1000" rows="2" /></label>
      <label><span class="field-label">交通</span><input v-model="day.transportation" maxlength="128" /></label>
      <div v-for="(item, itemIndex) in (day.attractions || [])" :key="itemIndex" class="field-row">
        <label><span class="field-label">景点</span><input v-model="item.name" maxlength="128" required /></label>
        <label><span class="field-label">地址</span><input v-model="item.address" maxlength="255" /></label>
        <button type="button" class="btn-danger" @click="day.attractions.splice(itemIndex, 1)">移除</button>
      </div>
      <div v-for="(item, itemIndex) in (day.meals || [])" :key="`meal-${itemIndex}`" class="field-row">
        <label><span class="field-label">餐饮</span><input v-model="item.name" maxlength="128" required /></label>
        <label><span class="field-label">地址</span><input v-model="item.address" maxlength="255" /></label>
        <button type="button" class="btn-danger" @click="day.meals.splice(itemIndex, 1)">移除</button>
      </div>
      <label v-if="day.hotel"><span class="field-label">住宿</span><input v-model="day.hotel.name" maxlength="128" /></label>
      <button type="button" class="btn-ghost" @click="addAttraction(day)">添加景点</button>
    </article>
    <div class="actions"><button type="submit" class="btn-coral" :disabled="busy === 'edit'">{{ busy === 'edit' ? '保存中…' : '保存行程' }}</button><button type="button" class="btn-ghost" @click="editDraft = null">取消</button></div>
  </form>

  <section v-if="days.length && !editDraft" class="trip-route-section" aria-labelledby="trip-route-title">
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
          <figure v-if="dayPhoto(day)" class="route-day-cover">
            <img :src="dayPhoto(day)" :alt="`${day.attractions[0].name}小红书旅行参考图`" loading="lazy" referrerpolicy="no-referrer" />
            <figcaption>小红书旅行参考 · {{ day.attractions[0].name }}</figcaption>
          </figure>
          <h3>Day {{ day.day_index != null ? Number(day.day_index) + 1 : idx + 1 }} · {{ day.date || '日期待定' }}</h3>
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
      <RouterLink class="btn-link btn-coral" to="/map">重新规划</RouterLink>
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
          <span>{{ stop.type }}</span><strong>{{ stop.name }}</strong><small>AI 建议 · 未预订 · {{ stop.address || departureDay.city || plan.city }} · 去导航 <ArrowRight :size="12" :stroke-width="2.4" aria-hidden="true" /></small>
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
        全屏地图 <ArrowRight :size="14" :stroke-width="2.2" aria-hidden="true" />
      </RouterLink>
    </div>
    <TravelMap3D
      ref="mapRef"
      :city="plan.city"
      height="340px"
      compact
      :show-pois="true"
      :initial-track-points="routeIntent?.nodes || []"
      style="margin-top: 12px;"
    />
    <div v-if="routeIntent" class="saved-route-strip" aria-label="原始地图路线">
      <button v-for="node in routeIntent.nodes" :key="node.order" type="button" @click="focusRouteNode(node)">
        <b>{{ String(node.order).padStart(2, '0') }}</b>
        <span>{{ node.name }}</span>
      </button>
    </div>
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
      <input v-model.trim="expenseForm.title" required maxlength="128" aria-label="花费内容" placeholder="例如：西湖边午餐" />
      <input v-model="expenseForm.amount" required type="number" min="0.01" max="1000000" step="0.01" inputmode="decimal" aria-label="花费金额" placeholder="金额" />
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
        <RouterLink :to="`/inspirations/${source.post_id}`">原帖 <ArrowRight :size="13" :stroke-width="2.2" aria-hidden="true" /></RouterLink>
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

    <div v-if="comfortData.comfort_class" class="trip-comfort-model">
      <div class="trip-comfort-result">
        <span class="trip-model-badge"><BrainCircuit :size="14" aria-hidden="true" />{{ modelVersionLabel }}</span>
        <p>模型判断</p>
        <strong><component :is="predictedComfort.icon" :size="22" aria-hidden="true" />{{ predictedComfort.label }}</strong>
        <small>置信度 {{ percent(comfortData.confidence) }}</small>
      </div>
      <div class="trip-comfort-probabilities" aria-label="舒适度预测概率">
        <div v-for="item in comfortProbabilities" :key="item.key">
          <span>{{ item.label }} <b>{{ percent(item.value) }}</b></span>
          <i><span :style="{ width: percent(item.value) }" /></i>
        </div>
      </div>
      <div class="trip-comfort-feedback">
        <p>回来后，实际感受如何？</p>
        <div role="group" aria-label="实际舒适度反馈">
          <button
            v-for="item in comfortProbabilities"
            :key="item.key"
            type="button"
            :class="{ 'is-selected': comfortFeedback.actual_label === item.key }"
            :disabled="!feedbackAllowed || busy === 'comfort-feedback'"
            @click="saveComfortFeedback(item.key)"
          ><component :is="item.icon" :size="15" aria-hidden="true" />{{ item.label }}</button>
        </div>
        <small v-if="comfortFeedback.submitted">已记录，可重新选择修正。</small>
        <small v-else-if="!feedbackAllowed">行程结束后开放反馈。</small>
        <small v-if="feedbackError" class="error-line">{{ feedbackError }}</small>
      </div>
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
          >问怎么调整 <ArrowRight :size="13" :stroke-width="2.2" aria-hidden="true" /></button>
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
          <button type="button" class="text-action text-action--primary" @click="replanWith(item.reply)">按这条建议重新规划 <ArrowRight :size="13" :stroke-width="2.2" aria-hidden="true" /></button>
        </div>
      </template>
    </div>
  </section>

    <section class="chapter-bridge" data-reveal>
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章 · 07 记录</p>
        <h2 class="chapter-bridge-title">这趟走完，写成回忆</h2>
        <p class="chapter-bridge-lead">行程落幕，回忆开始。把照片、感受、意外收进旅行记录或写成游记，给未来留一份可翻阅的注脚。</p>
      </div>
      <div style="display:flex; gap:12px; flex-wrap:wrap;">
        <RouterLink class="chapter-bridge-cta" to="/memories">
          <span>去写旅行记录</span>
          <ArrowRight :size="18" :stroke-width="2.2" />
        </RouterLink>
        <RouterLink class="chapter-bridge-cta btn-ghost" to="/journals">
          <span>去写旅行游记</span>
          <ArrowRight :size="18" :stroke-width="2.2" />
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.saved-route-strip {
  display: flex;
  overflow-x: auto;
  gap: 8px;
  margin-top: 10px;
  padding: 2px 1px 5px;
  scrollbar-width: thin;
}
.saved-route-strip button {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 7px;
  padding: 6px 10px 6px 6px;
  border: 1px solid var(--tm-line);
  border-radius: 999px;
  background: var(--tm-paper);
  color: var(--tm-ink);
  cursor: pointer;
  font-size: 11px;
  font-weight: 800;
}
.saved-route-strip button:hover,
.saved-route-strip button:focus-visible { border-color: var(--tm-accent); outline: 2px solid var(--tm-accent-soft); outline-offset: 2px; }
.saved-route-strip b { display: grid; width: 25px; height: 25px; place-items: center; border-radius: 50%; background: var(--tm-accent); color: #160d05; font: 900 10px/1 var(--font-mono, monospace); }
.route-day-cover { position: relative; margin: 0 0 16px; overflow: hidden; border-radius: var(--tm-radius-control); }
.route-day-cover img { display: block; width: 100%; height: clamp(150px, 22vw, 230px); object-fit: cover; }
.route-day-cover figcaption { position: absolute; right: 10px; bottom: 9px; padding: 5px 8px; border-radius: var(--tm-radius-pill); background: rgba(10, 12, 16, .74); color: #fff; font-size: 11px; font-weight: 700; }
</style>
