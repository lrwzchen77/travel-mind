<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { useRoute, useRouter, RouterLink } from 'vue-router';
import { http } from '../api/http.js';
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

const prefOptions = ['湖景', '美食', '轻松', '亲子', '夜景', '拍照', '徒步', '博物馆', '购物'];

const form = reactive({
  city: '杭州',
  start_date: '2026-08-01',
  end_date: '2026-08-02',
  travel_days: 2,
  transportation: '公共交通',
  accommodation: '舒适型酒店',
  budget: '3000',
  preferences: ['湖景', '美食', '轻松'],
  free_text_input: '节奏轻松，适合第一次到杭州。',
  language: 'zh',
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

async function submit() {
  taskAbortController?.abort();
  const controller = new AbortController();
  taskAbortController = controller;
  loading.value = true;
  error.value = '';
  result.value = null;
  try {
    const payload = {
      ...form,
      travel_days: Number(form.travel_days),
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

onMounted(() => {
  if (route.query.city) {
    form.city = String(route.query.city);
    const vision = route.query.vision ? String(route.query.vision) : '';
    const source = route.query.model === 'local' ? '本地图片模型' : '图片场景';
    const notes = [];
    if (vision) notes.push(`${source}判断我喜欢${vision}，请安排相似体验并留意相应风险`);
    if (route.query.poi) notes.push(`希望围绕${String(route.query.poi)}安排邻近景点，减少折返`);
    form.free_text_input = notes.length
      ? `想去${form.city}，${notes.join('；')}。`
      : `想去${form.city}玩一玩，节奏轻松一点。`;
  }
  const requestedPreferences = [
    route.query.preference,
    ...String(route.query.preferences || '').split(','),
  ].map((item) => String(item || '').trim()).filter(Boolean);
  for (const preference of requestedPreferences) {
    if (prefOptions.includes(preference) && !form.preferences.includes(preference)) {
      form.preferences.push(preference);
    }
  }
});

onUnmounted(() => taskAbortController?.abort());
</script>

<template>
  <section class="page-intro">
    <p class="eyebrow">规划行程</p>
    <h1>这一趟，怎么玩？</h1>
    <p>说清楚去哪、几天、大概花多少就够了——剩下的交给 Travel Mind 排成可执行日程。</p>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <section class="planner-map-block glass-panel">
    <div class="planner-map-head">
      <div>
        <h2>在地图上确认目的地</h2>
        <p class="panel-hint" style="margin-bottom: 0;">先看清城市位置，也可以直接在下方写下目的地。</p>
      </div>
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
  </section>

  <section class="planner-layout" style="margin-top: 22px;">
    <form class="glass-panel field-stack" @submit.prevent="submit">
      <h2>写下你的旅行愿望</h2>
      <p class="panel-hint">不用填得很完美，像和朋友聊天一样说清想法就行。</p>

      <div class="field-row">
        <div>
          <label class="field-label">想去哪座城</label>
          <input v-model="form.city" placeholder="例如：杭州、成都、厦门" />
        </div>
        <div>
          <label class="field-label">玩几天</label>
          <input v-model="form.travel_days" type="number" min="1" max="30" placeholder="2" />
        </div>
      </div>

      <div class="field-row">
        <div>
          <label class="field-label">出发日期</label>
          <input v-model="form.start_date" type="date" />
        </div>
        <div>
          <label class="field-label">返程日期</label>
          <input v-model="form.end_date" type="date" />
        </div>
      </div>

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
        <label class="field-label">大概预算（元）</label>
        <input v-model="form.budget" type="number" min="0" step="100" placeholder="例如 3000" />
      </div>

      <div>
        <label class="field-label">这趟更想要…</label>
        <div class="chip-row">
          <button
            v-for="tag in prefOptions"
            :key="tag"
            type="button"
            class="chip-choice"
            :class="{ 'is-on': form.preferences.includes(tag) }"
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
          placeholder="例如：第一次去、带老人、想少走路、爱吃辣…"
        />
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
        {{ task?.progress_text || task?.message || '填好左边，点生成，这里会出现逐日路线。' }}
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
</template>
