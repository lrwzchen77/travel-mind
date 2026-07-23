<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { useRoute, useRouter, RouterLink } from 'vue-router';
import { http } from '../api/http.js';
import { resourceApi } from '../api/resources.js';
import { tripApi } from '../api/trip.js';
import { authSession } from '../auth/session.js';
import {
  normalizeTripTaskStatus,
  TripTaskTimeoutError,
  waitForTripTask,
} from '../api/tripTask.js';
import PublicTravelDataPanel from '../components/PublicTravelDataPanel.vue';
import { consumerText } from '../data/consumerText.js';
import { supportedPlanningCities, supportsPlanning } from '../data/planningSupport.js';
import { normalizeRouteIntent, ROUTE_INTENT_KEY } from '../map/trackEditor.js';

const router = useRouter();
const route = useRoute();
const loading = ref(false);
const error = ref('');
const task = ref(null);
const result = ref(null);
const routeIntent = ref(null);
let taskAbortController = null;
const DRAFT_KEY = 'travelmind.planning-draft';

const prefOptions = ['湖景', '美食', '轻松', '亲子', '夜景', '拍照', '徒步', '博物馆', '购物'];

function localDate(offsetDays = 0) {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() + offsetDays);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

const form = reactive({
  city: '杭州',
  start_date: localDate(7),
  travel_days: 2,
  transportation: '公共交通',
  accommodation: '舒适型酒店',
  budget: '3000',
  adults: 2,
  children: 0,
  preferences: ['湖景', '美食', '轻松'],
  free_text_input: '节奏轻松，适合第一次到杭州。',
  language: 'zh',
  inspiration_ids: [],
});

const today = localDate();
const endDate = computed(() => {
  const daysCount = Number(form.travel_days);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(form.start_date) || !Number.isInteger(daysCount) || daysCount < 1) return '';
  const date = new Date(`${form.start_date}T00:00:00`);
  if (Number.isNaN(date.getTime())) return '';
  date.setDate(date.getDate() + daysCount - 1);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
});
const days = computed(() => result.value?.data?.days || []);
const budget = computed(() => result.value?.data?.budget || {});
const progress = computed(() => Number(task.value?.progress || 0));
const taskStatus = computed(() => normalizeTripTaskStatus(task.value?.status));
const citySupported = computed(() => supportsPlanning(form.city));
const resultSuggestion = computed(() => consumerText(result.value?.data?.overall_suggestions) || '已排好重点安排');

const statusLabel = computed(() => {
  const map = {
    ready: '准备好了',
    pending: '稍等片刻',
    processing: '正在为你排程…',
    running: '正在为你排程…',
    completed: '行程已就绪',
    failed: '这次没排成功',
    background: '还在慢慢生成',
  };
  return map[taskStatus.value] || '准备好了';
});

function togglePref(tag) {
  const i = form.preferences.indexOf(tag);
  if (i >= 0) form.preferences.splice(i, 1);
  else form.preferences.push(tag);
}

function values(value) {
  return String(value || '').split(/[,，、\s]+/).map((item) => item.trim()).filter(Boolean);
}

function appendNote(note) {
  const text = String(note || '').trim().slice(0, 1500);
  if (text && !form.free_text_input.includes(text)) {
    form.free_text_input = [form.free_text_input.trim(), text].filter(Boolean).join('\n');
  }
}

function applyProfile(profile) {
  const preference = profile?.preference || {};
  const preferredCity = values(preference.preferred_city)[0];
  if (preferredCity) form.city = preferredCity;
  if (preference.transportation) form.transportation = String(preference.transportation);
  if (preference.hotel_level) form.accommodation = String(preference.hotel_level);
  const budgets = { economy: '1500', medium: '3000', premium: '6000' };
  if (budgets[preference.budget_level]) form.budget = budgets[preference.budget_level];

  const unmatched = [];
  for (const item of [...values(preference.travel_style), ...values(preference.preferred_tags)]) {
    const matched = prefOptions.find((tag) => item.includes(tag) || tag.includes(item));
    if (matched && !form.preferences.includes(matched)) form.preferences.push(matched);
    else if (!matched) unmatched.push(item);
  }
  appendNote([
    unmatched.length ? `其他旅行偏好：${unmatched.join('、')}` : '',
    preference.diet_preference ? `饮食偏好：${preference.diet_preference}` : '',
  ].filter(Boolean).join('；'));
}

function restoreDraft() {
  try {
    const draft = JSON.parse(window.sessionStorage.getItem(DRAFT_KEY) || 'null');
    window.sessionStorage.removeItem(DRAFT_KEY);
    if (!draft) return;
    for (const key of Object.keys(form)) {
      if (Object.prototype.hasOwnProperty.call(draft, key)) form[key] = draft[key];
    }
  } catch {
    // 会话存储不可用时保留页面默认值。
  }
}

function applyQuery() {
  const directFields = ['city', 'start_date', 'travel_days', 'transportation', 'accommodation', 'budget', 'adults', 'children'];
  for (const field of directFields) {
    if (route.query[field] != null && String(route.query[field]).trim()) form[field] = String(route.query[field]);
  }
  if (route.query.free_text_input) form.free_text_input = String(route.query.free_text_input).slice(0, 1500);

  const vision = route.query.vision ? String(route.query.vision) : '';
  if (vision) appendNote(`照片场景偏好：喜欢${vision}，请安排相似的旅行感觉并留意相应风险。`);
  if (route.query.poi) appendNote(`希望围绕${String(route.query.poi)}安排邻近景点，减少折返。`);
  appendNote(route.query.note);
  appendNote(route.query.assistant ? `AI 对话补充：${String(route.query.assistant).slice(0, 800)}` : '');

  form.inspiration_ids = String(route.query.inspirationIds || '').split(',').map(Number).filter(Boolean).slice(0, 5);
  const explicitPreferences = String(route.query.preferences || '').split(',')
    .map((item) => item.trim()).filter((item) => prefOptions.includes(item));
  if (explicitPreferences.length) form.preferences = [...explicitPreferences];
  const requestedPreferences = [route.query.preference]
    .map((item) => String(item || '').trim()).filter(Boolean);
  for (const preference of requestedPreferences) {
    if (prefOptions.includes(preference) && !form.preferences.includes(preference)) form.preferences.push(preference);
  }
}

function restoreRouteIntent() {
  if (String(route.query.route || '') !== '1') return;
  try {
    const value = JSON.parse(window.sessionStorage.getItem(ROUTE_INTENT_KEY) || 'null');
    routeIntent.value = normalizeRouteIntent(value, form.city);
    if (!routeIntent.value) {
      window.sessionStorage.removeItem(ROUTE_INTENT_KEY);
      error.value = '这份路线草稿已失效，回到地图重新画一条就好。';
    }
  } catch {
    routeIntent.value = null;
    error.value = '这份路线草稿无法读取，回到地图重新画一条就好。';
  }
}

function planningConstraints() {
  return [
    `同行人数：${Number(form.adults)} 位成人、${Number(form.children)} 位儿童`,
    '预算范围：仅计算目的地内的吃住行游，不包含往返目的地的大交通',
  ].join('\n');
}

async function submit() {
  const travelDays = Number(form.travel_days);
  if (!routeIntent.value) {
    error.value = '请先回到地图选好至少两个路线节点。';
    return;
  }
  if (!form.city.trim()) {
    error.value = '请填写目的地城市';
    return;
  }
  if (!citySupported.value) {
    error.value = `当前只对 ${supportedPlanningCities.join('、')} 开放完整吃住玩规划。${form.city.trim()} 的真实资源还在补充，本次不会用占位商户生成行程。`;
    return;
  }
  if (routeIntent.value && routeIntent.value.city !== form.city.trim()) {
    error.value = '路线草稿与目的地不一致，请返回地图重新确认目的地。';
    return;
  }
  if (form.city.trim().length > 60) {
    error.value = '目的地请控制在 60 个字以内';
    return;
  }
  const adults = Number(form.adults);
  const children = Number(form.children);
  if (!Number.isInteger(adults) || adults < 1 || adults > 20 || !Number.isInteger(children) || children < 0 || children > 20) {
    error.value = '请填写 1 至 20 位成人、0 至 20 位儿童';
    return;
  }
  const budgetValue = Number(form.budget);
  if (form.budget !== '' && (!Number.isFinite(budgetValue) || budgetValue < 0 || budgetValue > 100000000)) {
    error.value = '预算请填写 0 至 1 亿元之间的金额';
    return;
  }
  if (form.free_text_input.length > 1500) {
    error.value = '补充要求请控制在 1500 个字以内';
    return;
  }
  if (!Number.isInteger(travelDays) || travelDays < 1 || travelDays > 30) {
    error.value = '游玩天数需为 1 到 30 天';
    return;
  }
  if (!endDate.value || form.start_date < today) {
    error.value = '出发日期不能早于今天';
    return;
  }
  if (!authSession.isLoggedIn()) {
    try {
      window.sessionStorage.setItem(DRAFT_KEY, JSON.stringify({ ...form, travel_days: travelDays }));
    } catch {
      // 浏览器禁用会话存储时仍允许用户继续登录。
    }
    await router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  taskAbortController?.abort();
  const controller = new AbortController();
  taskAbortController = controller;
  loading.value = true;
  error.value = '';
  result.value = null;
  try {
    const { adults: adultCount, children: childCount, ...acceptedFields } = form;
    const payload = {
      ...acceptedFields,
      end_date: endDate.value,
      travel_days: travelDays,
      preferences: [...form.preferences],
      free_text_input: [form.free_text_input.trim(), planningConstraints()].filter(Boolean).join('\n'),
      ...(routeIntent.value ? { route_intent: routeIntent.value } : {}),
    };
    task.value = await tripApi.submitPlan(payload);
    const finalState = await waitForTripTask({
      taskId: task.value.task_id,
      wsUrl: task.value.ws_url,
      loadStatus: (taskId) => tripApi.status(taskId),
      apiBaseUrl: http.defaults.baseURL,
      token: authSession.token(),
      signal: controller.signal,
      onUpdate: (state) => {
        task.value = state;
      },
    });
    result.value = finalState.result;
    if (result.value) {
      try {
        window.sessionStorage.removeItem(ROUTE_INTENT_KEY);
      } catch {
        // 规划已完成，不让存储异常影响结果展示。
      }
    }
  } catch (err) {
    if (err?.name === 'AbortError') return;
    if (err instanceof TripTaskTimeoutError) {
      task.value = {
        ...(err.lastState || task.value),
        status: 'background',
        progress_text: '这趟排得稍久一些，还在继续生成。可以先去「我的行程」稍后查看。',
      };
      return;
    }
    error.value = err?.response?.data?.msg || err?.message || '规划没成功，稍后再试一次';
  } finally {
    if (taskAbortController === controller) {
      loading.value = false;
      taskAbortController = null;
    }
  }
}

function openSavedPlan() {
  const id = result.value?.plan_id;
  if (id) router.push(`/trip/${id}`);
}

onMounted(async () => {
  if (authSession.isLoggedIn()) {
    try {
      applyProfile(await resourceApi.getProfile());
    } catch {
      // 偏好服务不可用不阻断规划。
    }
  }
  restoreDraft();
  applyQuery();
  restoreRouteIntent();
});

onUnmounted(() => taskAbortController?.abort());
</script>

<template>
  <section class="page-intro confirmation-intro">
    <div>
      <p class="eyebrow">{{ routeIntent ? '路线确认 · 最后一步' : '路线规划 · 从地图开始' }}</p>
      <h1>{{ routeIntent ? '路线圈好了，再补几项' : '先圈路线，再生成行程' }}</h1>
      <p class="lead">{{ routeIntent ? '地点和顺序已经从地图带过来，这里只确认出行信息，然后交给 AI 排成正式日程。' : '这一步只负责确认路线；地点、顺序和每一站的偏好都在地图上完成。' }}</p>
    </div>
    <RouterLink class="btn-link btn-ghost" :to="{ path: '/map', query: { city: form.city } }">← 返回地图调整</RouterLink>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <section v-if="!routeIntent" class="route-required glass-panel">
    <span aria-hidden="true">⌁</span>
    <p class="eyebrow">还缺一条路线</p>
    <h2>先在地图上选好至少两个节点</h2>
    <p>地点、顺序、节点备注和偏好都从地图进入规划；确认页不再重复替你选择目的地。</p>
    <RouterLink class="btn-link btn-coral" :to="{ path: '/map', query: { city: form.city } }">去地图画路线</RouterLink>
  </section>

  <section v-else class="planner-layout route-confirm-layout">
    <form class="glass-panel field-stack" @submit.prevent="submit">
      <div class="confirm-section-title"><span>补充信息</span><h2>只补 AI 还不知道的事</h2></div>

      <div class="route-locked-summary">
        <div>
          <span>地图路线</span>
          <strong>{{ routeIntent.city }} · {{ routeIntent.nodes.length }} 个节点</strong>
        </div>
        <small>{{ endDate ? `预计 ${endDate} 结束` : '结束日期将自动计算' }}</small>
      </div>

      <div class="field-row">
        <div>
          <label class="field-label" for="planning-start-date">哪天开始</label>
          <input id="planning-start-date" v-model="form.start_date" type="date" :min="today" required />
        </div>
        <div>
          <label class="field-label" for="planning-days">玩几天</label>
          <input id="planning-days" v-model.number="form.travel_days" type="number" min="1" max="30" placeholder="2" required />
        </div>
      </div>

      <div class="field-row">
        <div class="traveller-counts">
          <span class="field-label">几个人</span>
          <div class="field-row">
            <label><small>成人</small><input id="planning-adults" v-model.number="form.adults" type="number" min="1" max="20" required /></label>
            <label><small>儿童</small><input id="planning-children" v-model.number="form.children" type="number" min="0" max="20" required /></label>
          </div>
        </div>
        <div>
          <label class="field-label" for="planning-budget">市内行程总预算（元）</label>
          <input id="planning-budget" v-model="form.budget" type="number" min="0" step="100" placeholder="例如 3000" />
          <small class="panel-hint">不含往返目的地的大交通</small>
        </div>
      </div>

      <details class="planner-more">
        <summary>按需补充：交通、住宿和整趟偏好</summary>
        <div class="planner-more-fields">
          <div class="field-row">
            <div>
              <label class="field-label" for="planning-transport">到达后怎么走</label>
              <input id="planning-transport" v-model="form.transportation" maxlength="100" placeholder="公共交通 / 自驾 / 打车" />
            </div>
            <div>
              <label class="field-label" for="planning-accommodation">住哪里</label>
              <input id="planning-accommodation" v-model="form.accommodation" maxlength="100" placeholder="舒适型酒店 / 民宿" />
            </div>
          </div>

          <div>
            <span class="field-label">整趟更想要…</span>
            <div class="chip-row" role="group" aria-label="旅行偏好">
              <button
                v-for="tag in prefOptions"
                :key="tag"
                type="button"
                class="chip-choice"
                :class="{ 'is-on': form.preferences.includes(tag) }"
                :aria-pressed="form.preferences.includes(tag)"
                @click="togglePref(tag)"
              >
                {{ tag }}
              </button>
            </div>
          </div>

          <div>
            <label class="field-label" for="planning-notes">还有什么想补充的</label>
            <textarea
              id="planning-notes"
              v-model="form.free_text_input"
              maxlength="1500"
              rows="3"
              spellcheck="false"
              placeholder="例如：第一次去、想少走路、爱吃辣…"
            />
          </div>
        </div>
      </details>

      <div v-if="form.inspiration_ids.length" class="planner-inspiration-note">
        <strong>已引用 {{ form.inspiration_ids.length }} 篇社区分享</strong>
        <RouterLink class="text-link" to="/inspiration-bag">调整灵感包 →</RouterLink>
      </div>

      <div class="actions">
        <button type="submit" class="btn-coral" :disabled="loading">
          {{ loading ? '正在为你排程…' : `按这 ${routeIntent.nodes.length} 个节点生成行程` }}
        </button>
      </div>
    </form>

    <div class="glass-panel route-review-panel" :class="{ 'is-route-draft': !result }">
      <div class="planner-result-head">
        <div><span>{{ result ? 'AI 已排程' : '地图路线' }}</span><h2>{{ result ? '你的行程草稿' : '确认节点顺序' }}</h2></div>
        <span class="badge" :class="{ 'badge-ok': taskStatus === 'completed', 'badge-warn': loading }">
          {{ loading || result ? statusLabel : `${routeIntent.nodes.length} 节点` }}
        </span>
      </div>

      <div v-if="loading || task" class="progress-bar" :class="{ 'is-live': loading }" aria-hidden="true">
        <span :style="{ width: `${loading && progress < 8 ? 8 : Math.min(100, progress)}%` }" />
      </div>
      <p v-if="loading || task" class="progress-text">
        {{ task?.progress_text || task?.message }}
      </p>

      <section v-if="!result" class="route-review" aria-label="待确认的路线节点">
        <ol>
          <li v-for="node in routeIntent.nodes" :key="`${node.order}-${node.longitude}-${node.latitude}`">
            <b>{{ String(node.order).padStart(2, '0') }}</b>
            <div>
              <strong>{{ node.name }}</strong>
              <span>{{ node.type === 'free_point' ? '自定义途经区域' : node.kind || '地图地点' }}</span>
              <p v-if="node.note">{{ node.note }}</p>
              <div v-if="node.preferences?.length" class="route-node-prefs">
                <i v-for="preference in node.preferences" :key="preference">{{ preference }}</i>
              </div>
            </div>
          </li>
        </ol>
        <p class="route-order-note">{{ routeIntent.mode === 'strict_order' ? 'AI 将严格按当前顺序安排。' : 'AI 会尽量按当前顺序安排，仅在营业时间、距离或节奏冲突时小幅调整。' }}</p>
        <RouterLink class="text-link" :to="{ path: '/map', query: { city: routeIntent.city } }">返回地图修改节点 →</RouterLink>
      </section>

      <div v-if="result" class="trip-summary" style="margin-top: 20px;">
        <p class="trust-note">以下为 AI 规划建议，不代表已预订。价格、库存、营业与预约信息请在出发前向服务方复核。</p>
        <article class="trip-summary-card">
          <span>目的地</span>
          <strong>{{ result.data.city }}</strong>
          <p>{{ result.data.start_date }} — {{ result.data.end_date }}</p>
        </article>
        <article class="trip-summary-card">
          <span>预算约</span>
          <strong>¥{{ budget.total || 0 }}</strong>
          <p>可在详情里再细聊</p>
        </article>
        <article class="trip-summary-card">
          <span>日程</span>
          <strong>{{ days.length }} 天</strong>
          <p>{{ resultSuggestion }}</p>
        </article>
      </div>

      <div v-if="days.length" class="route-timeline" style="margin-top: 8px;">
        <article v-for="(day, idx) in days" :key="day.day_index ?? idx" class="route-day">
          <div class="route-axis"><span class="route-node" /></div>
          <div class="route-card">
            <h3>Day {{ day.day_index || idx + 1 }} · {{ day.date }}</h3>
            <p class="day-meta">{{ day.city }} · {{ day.transportation || '轻松一天' }}</p>
            <div class="route-chips">
              <span
                v-for="item in (day.attractions || [])"
                :key="`a-${item.name}`"
                class="chip chip-accent"
              >{{ item.name }}</span>
              <span v-for="item in (day.meals || [])" :key="`m-${item.name}`" class="chip">{{ item.name }}</span>
              <span v-if="day.hotel?.name" class="chip">住 {{ day.hotel.name }}</span>
            </div>
          </div>
        </article>
      </div>

      <PublicTravelDataPanel v-if="result?.data?.public_data?.length" :items="result.data.public_data" />

      <div v-if="result" class="actions" style="margin-top: 18px;">
        <button type="button" class="btn-coral" @click="openSavedPlan">打开完整行程</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.confirmation-intro {
  display: flex;
  width: 100%;
  max-width: none;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
}

.confirmation-intro > div { min-width: 0; }
.confirmation-intro .btn-link { flex: 0 0 auto; }
.confirmation-intro h1 { max-width: none; }

.route-required {
  display: grid;
  max-width: 760px;
  min-height: 360px;
  margin: 24px auto 0;
  padding: clamp(32px, 6vw, 72px);
  place-items: center;
  align-content: center;
  text-align: center;
}

.route-required > span {
  display: grid;
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  place-items: center;
  border: 1px solid #efc3a3;
  border-radius: 50%;
  background: #fff3e8;
  color: #e87022;
  font: 800 34px/1 var(--font-display);
}

.route-required h2 { margin: 4px 0 10px; font-size: clamp(23px, 3vw, 32px); }
.route-required > p:not(.eyebrow) { max-width: 520px; margin: 0 0 24px; color: #6f6a63; line-height: 1.7; }

.route-confirm-layout { grid-template-columns: minmax(320px, .88fr) minmax(400px, 1.12fr); }
.route-confirm-layout > .field-stack { gap: 18px; }

.confirm-section-title { padding-bottom: 14px; border-bottom: 1px solid #e7dfd4; }
.confirm-section-title span,
.planner-result-head > div > span {
  display: block;
  margin-bottom: 6px;
  color: #e87022;
  font: 800 10px/1.2 var(--font-mono);
  letter-spacing: .12em;
}
.confirm-section-title h2 { margin: 0; color: #173f50; font-size: 24px; }

.route-locked-summary {
  display: flex;
  min-height: 72px;
  padding: 13px 15px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #ded6cb;
  border-radius: 10px;
  background: #f8f5ef;
}
.route-locked-summary > div { display: grid; gap: 4px; }
.route-locked-summary span { color: #e87022; font: 800 10px/1.2 var(--font-mono); letter-spacing: .08em; }
.route-locked-summary strong { color: #173f50; font: 800 17px/1.2 var(--font-display); }
.route-locked-summary small { color: #817970; text-align: right; }

.route-review-panel { min-height: 0; }
.route-review-panel.is-route-draft {
  position: sticky;
  top: calc(var(--header-h, 72px) + 18px);
  overflow: hidden;
  border-color: #234f5f;
  background: linear-gradient(145deg, #173f50 0%, #214e5d 100%);
  color: #fffaf1;
  box-shadow: 0 22px 50px rgba(23, 63, 80, .2);
}
.route-review-panel.is-route-draft::before { border-top-color: #f47a2a; }
.is-route-draft .planner-result-head h2 { color: #fffaf1; }
.is-route-draft .badge { border-color: rgba(255, 250, 241, .16); background: rgba(255, 250, 241, .1); color: #fffaf1; }

.route-review { margin-top: 22px; }
.route-review ol { display: grid; gap: 0; margin: 0; padding: 0; list-style: none; }
.route-review li {
  position: relative;
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 14px;
  min-height: 86px;
  padding-bottom: 22px;
}
.route-review li:not(:last-child)::before {
  content: "";
  position: absolute;
  top: 38px;
  bottom: 0;
  left: 20px;
  width: 2px;
  background: linear-gradient(#e87022, rgba(244, 173, 66, .24));
}
.route-review li > b {
  position: relative;
  z-index: 1;
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 2px solid #f4ad42;
  border-radius: 50%;
  background: #173f50;
  color: #fffaf1;
  font: 900 11px/1 var(--font-mono);
  box-shadow: 0 0 0 5px rgba(244, 173, 66, .1);
}
.route-review li > div { min-width: 0; padding-top: 1px; }
.route-review li strong { display: block; color: #fffaf1; font: 800 17px/1.3 var(--font-display); }
.route-review li > div > span { display: block; margin-top: 4px; color: rgba(255, 250, 241, .58); font-size: 11px; }
.route-review li p { margin: 10px 0 0; color: rgba(255, 250, 241, .84); font-size: 13px; line-height: 1.55; }

.route-node-prefs { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.route-node-prefs i {
  padding: 5px 8px;
  border: 1px solid rgba(244, 173, 66, .3);
  border-radius: 999px;
  background: rgba(244, 173, 66, .09);
  color: #ffd08b;
  font-size: 10px;
  font-style: normal;
  font-weight: 800;
}
.route-order-note {
  margin: 2px 0 12px;
  padding: 12px 14px;
  border-left: 3px solid #f4ad42;
  background: rgba(255, 250, 241, .07);
  color: rgba(255, 250, 241, .7);
  font-size: 12px;
  line-height: 1.6;
}
.is-route-draft .text-link { color: #ffd08b; }
.is-route-draft .text-link:focus-visible { outline-color: #ffd08b; }

@media (max-width: 980px) {
  .route-confirm-layout { grid-template-columns: 1fr; }
  .route-review-panel.is-route-draft { position: static; }
}

@media (max-width: 640px) {
  .confirmation-intro { display: grid; align-items: start; }
  .confirmation-intro .btn-link { justify-self: start; }
  .route-confirm-layout .field-row { grid-template-columns: 1fr; }
  .route-required { min-height: 300px; }
}
</style>
