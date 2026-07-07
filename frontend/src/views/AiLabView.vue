<script setup>
import { computed, reactive, ref } from 'vue';
import { aiApi } from '../api/ai.js';

const loading = ref('');
const error = ref('');
const vision = ref(null);
const trip = ref(null);
const content = ref(null);

const visionForm = reactive({
  image_url: 'https://example.com/west-lake-night-food.jpg',
  city: 'Hangzhou',
  resource_type: 'attraction',
});
const tripText = ref(JSON.stringify({
  transportation: '公共交通',
  budget: 3000,
  preferences: ['轻松', '美食'],
  days: [
    { date: '2026-08-01', city: 'Hangzhou', attractions: ['西湖', '灵隐寺', '河坊街'], weather: '晴' },
  ],
}, null, 2));
const contentForm = reactive({
  text: '西湖风景很好，适合亲子散步，但是节假日排队很久。',
  city: '杭州',
  attraction_name: '西湖',
  language: 'zh',
});

const tripData = computed(() => trip.value?.data || trip.value || {});
const visionData = computed(() => vision.value?.data || vision.value || {});
const contentData = computed(() => content.value?.data || content.value || {});

async function runVision() {
  await run('vision', async () => {
    vision.value = await aiApi.detectVision({ ...visionForm });
  });
}

async function runTrip() {
  await run('trip', async () => {
    trip.value = await aiApi.evaluateTrip(JSON.parse(tripText.value));
  });
}

async function runContent() {
  await run('content', async () => {
    content.value = await aiApi.analyzeContent({ ...contentForm });
  });
}

async function run(name, action) {
  loading.value = name;
  error.value = '';
  try {
    await action();
  } catch (err) {
    error.value = err?.message || 'Request failed';
  } finally {
    loading.value = '';
  }
}
</script>

<template>
  <section class="page-header">
    <h1>AI Lab</h1>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <section class="crud-layout">
    <form class="editor-panel" @submit.prevent="runVision">
      <h2>Vision Detection</h2>
      <input v-model="visionForm.image_url" placeholder="Image URL" />
      <input v-model="visionForm.city" placeholder="City" />
      <input v-model="visionForm.resource_type" placeholder="Resource type" />
      <div class="actions">
        <button type="submit">{{ loading === 'vision' ? 'Running' : 'Run' }}</button>
      </div>
      <pre v-if="vision">{{ JSON.stringify(visionData, null, 2) }}</pre>
    </form>

    <form class="editor-panel" @submit.prevent="runTrip">
      <h2>Trip Comfort</h2>
      <textarea v-model="tripText" rows="12" spellcheck="false"></textarea>
      <div class="actions">
        <button type="submit">{{ loading === 'trip' ? 'Running' : 'Run' }}</button>
      </div>
      <pre v-if="trip">{{ JSON.stringify(tripData, null, 2) }}</pre>
    </form>

    <form class="editor-panel" @submit.prevent="runContent">
      <h2>Text Analysis</h2>
      <textarea v-model="contentForm.text" rows="5" spellcheck="false"></textarea>
      <input v-model="contentForm.city" placeholder="City" />
      <input v-model="contentForm.attraction_name" placeholder="Attraction" />
      <div class="actions">
        <button type="submit">{{ loading === 'content' ? 'Running' : 'Run' }}</button>
      </div>
      <pre v-if="content">{{ JSON.stringify(contentData, null, 2) }}</pre>
    </form>
  </section>
</template>
