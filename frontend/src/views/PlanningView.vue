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
import TravelMap3D from '../components/map/AsyncTravelMap3D.vue';

const router = useRouter();
const route = useRoute();
const loading = ref(false);
const error = ref('');
const task = ref(null);
const result = ref(null);
let taskAbortController = null;
const DRAFT_KEY = 'travelmind.planning-draft';

const prefOptions = ['湖景', '美食', '轻松', '亲子', '夜景', '拍照', '徒步', '博物馆', '购物'];
const companionOptions = ['独自', '情侣', '朋友', '带孩子', '带老人'];
const companion = ref('');

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

function setCompanion(value) {
  companion.value = value;
  form.free_text_input = form.free_text_input.replace(/^同行人：.*(?:\n|$)/m, '').trim();
  appendNote(`同行人：${value}`);
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
  const directFields = ['city', 'start_date', 'travel_days', 'transportation', 'accommodation', 'budget'];
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

async function submit() {
  const travelDays = Number(form.travel_days);
  if (!form.city.trim()) {
    error.value = '请填写目的地城市';
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
    const payload = {
      ...form,
      end_date: endDate.value,
      travel_days: travelDays,
      preferences: [...form.preferences],
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
});

onUnmounted(() => taskAbortController?.abort());
</script>

<template>
  <section class="page-intro">
    <p class="eyebrow">规划行程</p>
    <h1>这一趟，怎么玩？</h1>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <section class="planner-layout">
    <form class="glass-panel field-stack" @submit.prevent="submit">
      <h2>写下你的旅行愿望</h2>

      <div class="field-row">
        <div>
          <label class="field-label">想去哪座城</label>
          <input v-model="form.city" placeholder="例如：杭州、成都、厦门" />
        </div>
        <div>
          <label class="field-label">玩几天</label>
          <input v-model.number="form.travel_days" type="number" min="1" max="30" placeholder="2" required />
        </div>
      </div>

      <div class="field-row">
        <div>
          <label class="field-label">出发日期</label>
          <input v-model="form.start_date" type="date" :min="today" required />
        </div>
        <div>
          <label class="field-label">预计返程</label>
          <input :value="endDate" type="date" readonly aria-label="根据出发日期和游玩天数自动计算的返程日期" />
        </div>
      </div>

      <div>
        <label class="field-label">大概预算（元）</label>
        <input v-model="form.budget" type="number" min="0" step="100" placeholder="例如 3000" />
      </div>

      <div>
        <span class="field-label">和谁一起</span>
        <div class="chip-row" role="group" aria-label="同行人快捷选择">
          <button
            v-for="item in companionOptions"
            :key="item"
            type="button"
            class="chip-choice"
            :class="{ 'is-on': companion === item }"
            :aria-pressed="companion === item"
            @click="setCompanion(item)"
          >
            {{ item }}
          </button>
        </div>
      </div>

      <details class="planner-more">
        <summary>更多要求：交通、住宿和旅行偏好</summary>
        <div class="planner-more-fields">
          <div class="field-row">
            <div>
              <label class="field-label">怎么出门</label>
              <input v-model="form.transportation" placeholder="公共交通 / 自驾 / 打车" />
            </div>
            <div>
              <label class="field-label">住哪里</label>
              <input v-model="form.accommodation" placeholder="舒适型酒店 / 民宿" />
            </div>
          </div>

          <div>
            <span class="field-label">这趟更想要…</span>
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
            <label class="field-label">还有什么想补充的</label>
            <textarea
              v-model="form.free_text_input"
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
          {{ loading ? '正在为你排程…' : '生成我的行程' }}
        </button>
      </div>
    </form>

    <div class="glass-panel">
      <div class="planner-result-head">
        <h2>你的行程草稿</h2>
        <span class="badge" :class="{ 'badge-ok': taskStatus === 'completed', 'badge-warn': loading }">
          {{ statusLabel }}
        </span>
      </div>

      <div class="progress-bar" :class="{ 'is-live': loading }" aria-hidden="true">
        <span :style="{ width: `${loading && progress < 8 ? 8 : Math.min(100, progress)}%` }" />
      </div>
      <p class="progress-text">
        {{ task?.progress_text || task?.message }}
      </p>

      <div v-if="result" class="trip-summary" style="margin-top: 20px;">
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
          <p>{{ result.data.overall_suggestions || '已排好重点安排' }}</p>
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

      <div v-if="!result && !loading" class="empty-state">
        <strong>行程还是空白的</strong>
        选好城市和标签，生成后这里会变成一条「走路地图」。
      </div>

      <div v-if="result" class="actions" style="margin-top: 18px;">
        <button type="button" class="btn-coral" @click="openSavedPlan">打开完整行程</button>
      </div>
    </div>
  </section>

  <details class="planner-map-block glass-panel">
    <summary>在地图上看看 {{ form.city }} 的位置和周边</summary>
    <div class="planner-map-head">
      <p class="panel-hint">地图用来辅助看距离，不影响上面的行程生成。</p>
      <RouterLink class="text-link" :to="{ path: '/map', query: { city: form.city } }">全屏地图 →</RouterLink>
    </div>
    <TravelMap3D
      style="margin-top: 14px;"
      :city="form.city"
      height="360px"
      compact
      :show-pois="true"
      @city-change="(c) => { form.city = c; }"
    />
  </details>
</template>
