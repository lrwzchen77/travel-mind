<script setup>
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { tripApi } from '../api/trip.js';

const router = useRouter();
const loading = ref(false);
const error = ref('');
const task = ref(null);
const result = ref(null);
const form = reactive({
  city: 'Hangzhou',
  start_date: '2026-08-01',
  end_date: '2026-08-02',
  travel_days: 2,
  transportation: '公共交通',
  accommodation: '舒适型酒店',
  budget: '3000',
  preferences: '湖景,美食,轻松',
  free_text_input: '节奏轻松，适合第一次到杭州。',
  language: 'zh',
});

const days = computed(() => result.value?.data?.days || []);
const budget = computed(() => result.value?.data?.budget || {});

async function submit() {
  loading.value = true;
  error.value = '';
  result.value = null;
  try {
    const payload = {
      ...form,
      travel_days: Number(form.travel_days),
      preferences: form.preferences.split(',').map((item) => item.trim()).filter(Boolean),
    };
    task.value = await tripApi.submitPlan(payload);
    await poll(task.value.task_id);
  } catch (err) {
    error.value = err?.response?.data?.msg || err?.message || 'Request failed';
  } finally {
    loading.value = false;
  }
}

async function poll(taskId) {
  for (let index = 0; index < 80; index += 1) {
    const status = await tripApi.status(taskId);
    task.value = status;
    if (status.status === 'COMPLETED') {
      result.value = status.result;
      return;
    }
    if (status.status === 'FAILED') {
      throw new Error(status.error || 'Planning failed');
    }
    await new Promise((resolve) => setTimeout(resolve, 700));
  }
  throw new Error('Planning timeout');
}

function openSavedPlan() {
  const id = result.value?.plan_id;
  if (id) {
    router.push(`/trip/${id}`);
  }
}
</script>

<template>
  <section class="page-header">
    <h1>Trip Planning</h1>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <section class="crud-layout">
    <form class="editor-panel" @submit.prevent="submit">
      <h2>Request</h2>
      <input v-model="form.city" placeholder="City" />
      <input v-model="form.start_date" placeholder="Start date" />
      <input v-model="form.end_date" placeholder="End date" />
      <input v-model="form.travel_days" placeholder="Days" />
      <input v-model="form.transportation" placeholder="Transportation" />
      <input v-model="form.accommodation" placeholder="Accommodation" />
      <input v-model="form.budget" placeholder="Budget" />
      <input v-model="form.preferences" placeholder="Preferences" />
      <textarea v-model="form.free_text_input" rows="5" spellcheck="false"></textarea>
      <input v-model="form.language" placeholder="Language" />
      <div class="actions">
        <button type="submit" :disabled="loading">{{ loading ? 'Planning' : 'Generate' }}</button>
      </div>
    </form>

    <div class="table-wrap">
      <div class="table-meta">
        {{ task?.status || 'READY' }} {{ task?.progress ? `${task.progress}%` : '' }}
      </div>
      <p>{{ task?.progress_text || task?.message || '' }}</p>
      <div v-if="result" class="status-grid">
        <article class="status-card">
          <h2>{{ result.data.city }}</h2>
          <p>{{ result.data.start_date }} - {{ result.data.end_date }}</p>
        </article>
        <article class="status-card">
          <h2>Budget {{ budget.total || 0 }}</h2>
          <p>{{ result.data.overall_suggestions }}</p>
        </article>
      </div>
      <table v-if="days.length">
        <thead>
          <tr>
            <th>Day</th>
            <th>City</th>
            <th>Attractions</th>
            <th>Meals</th>
            <th>Hotel</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="day in days" :key="day.day_index">
            <td>{{ day.date }}</td>
            <td>{{ day.city }}</td>
            <td>{{ (day.attractions || []).map((item) => item.name).join(', ') }}</td>
            <td>{{ (day.meals || []).map((item) => item.name).join(', ') }}</td>
            <td>{{ day.hotel?.name }}</td>
          </tr>
        </tbody>
      </table>
      <div v-if="result" class="actions">
        <button type="button" @click="openSavedPlan">Open Detail</button>
      </div>
    </div>
  </section>
</template>
