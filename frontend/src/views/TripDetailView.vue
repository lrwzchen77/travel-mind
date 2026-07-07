<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { tripApi } from '../api/trip.js';
import { aiApi } from '../api/ai.js';

const route = useRoute();
const router = useRouter();
const detail = ref(null);
const error = ref('');
const chatText = ref('预算是多少？');
const replies = ref([]);
const comfort = ref(null);

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
    error.value = err?.response?.data?.msg || err?.message || 'Request failed';
  }
}

async function copyPlan() {
  const copied = await tripApi.copy(route.params.id);
  router.push(`/trip/${copied.plan_id}`);
}

async function deletePlan() {
  await tripApi.remove(route.params.id);
  router.push('/trip-history');
}

async function chat() {
  const response = await tripApi.chat(route.params.id, chatText.value, replies.value);
  replies.value.push({ message: chatText.value, reply: response.reply });
  chatText.value = '';
}

onMounted(load);
</script>

<template>
  <section class="page-header">
    <h1>{{ plan.city || 'Trip Detail' }}</h1>
    <p>{{ plan.start_date }} - {{ plan.end_date }}</p>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <section v-if="detail" class="status-grid">
    <article class="status-card">
      <h2>Budget {{ budget.total || 0 }}</h2>
      <p>{{ plan.overall_suggestions }}</p>
    </article>
    <article class="status-card">
      <h2>Graph {{ detail.graph_data?.nodes?.length || 0 }}</h2>
      <p>{{ detail.graph_data?.edges?.length || 0 }} relations</p>
    </article>
    <article class="status-card">
      <h2>Comfort {{ comfortData.comfort_score || '-' }}</h2>
      <p>{{ comfortData.risk_level || comfort?.status || 'pending' }}</p>
    </article>
  </section>

  <div v-if="detail" class="actions" style="margin: 20px 0;">
    <button type="button" @click="copyPlan">Copy</button>
    <button type="button" @click="deletePlan">Delete</button>
  </div>

  <div v-if="days.length" class="table-wrap">
    <table>
      <thead>
        <tr>
          <th>Day</th>
          <th>Transport</th>
          <th>Attractions</th>
          <th>Meals</th>
          <th>Hotel</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="day in days" :key="day.day_index">
          <td>{{ day.date }}<br />{{ day.description }}</td>
          <td>{{ day.transportation }}</td>
          <td>{{ (day.attractions || []).map((item) => item.name).join(', ') }}</td>
          <td>{{ (day.meals || []).map((item) => item.name).join(', ') }}</td>
          <td>{{ day.hotel?.name }}</td>
        </tr>
      </tbody>
    </table>
  </div>

  <section v-if="detail" class="editor-panel" style="margin-top: 18px;">
    <h2>Trip Chat</h2>
    <textarea v-model="chatText" rows="3" spellcheck="false"></textarea>
    <div class="actions">
      <button type="button" @click="chat">Send</button>
    </div>
    <div v-for="item in replies" :key="item.reply" class="status-card" style="margin-top: 10px;">
      <h2>{{ item.message }}</h2>
      <p>{{ item.reply }}</p>
    </div>
  </section>
</template>
