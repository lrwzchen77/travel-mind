<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { resourceApi } from '../api/resources.js';
import { aiApi } from '../api/ai.js';

const route = useRoute();
const loading = ref(false);
const error = ref('');
const records = ref([]);
const total = ref(0);
const editingId = ref(null);
const formText = ref('{\n  "name": ""\n}');
const analysisResult = ref(null);
const filters = reactive({
  keyword: '',
  cityId: '',
  category: '',
  tag: '',
  ratingMin: '',
  ratingMax: '',
  userId: '',
  attractionId: '',
  targetId: '',
  targetType: '',
  analysisType: '',
  status: '',
});

const resourceKey = computed(() => route.meta.resourceKey);
const title = computed(() => route.meta.title);
const fields = computed(() => route.meta.fields || []);
const canToggleStatus = computed(() => route.meta.canToggleStatus !== false);
const isTravelNotes = computed(() => resourceKey.value === 'travel-notes');

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const data = await resourceApi.list(resourceKey.value, cleanFilters());
    records.value = data.records || [];
    total.value = data.total || 0;
  } catch (err) {
    error.value = err?.message || 'Request failed';
  } finally {
    loading.value = false;
  }
}

async function save() {
  const payload = JSON.parse(formText.value);
  if (editingId.value) {
    await resourceApi.update(resourceKey.value, editingId.value, payload);
  } else {
    await resourceApi.create(resourceKey.value, payload);
  }
  resetForm();
  await load();
}

async function remove(record) {
  await resourceApi.remove(resourceKey.value, record.id);
  await load();
}

async function toggleStatus(record) {
  await resourceApi.updateStatus(resourceKey.value, record.id, Number(record.status) === 1 ? 0 : 1);
  await load();
}

async function analyzeNote(record) {
  analysisResult.value = await aiApi.analyzeContent(
    {
      text: record.content || record.title || '',
      city: String(record.city_id || ''),
      attraction_name: String(record.attraction_id || ''),
      language: 'zh',
    },
    { targetId: record.id, targetType: 'travel_note' },
  );
}

function edit(record) {
  editingId.value = record.id;
  formText.value = JSON.stringify(record, null, 2);
}

function resetForm() {
  editingId.value = null;
  formText.value = '{\n  "name": ""\n}';
}

function cleanFilters() {
  return Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== '' && value !== null));
}

watch(() => route.fullPath, () => {
  resetForm();
  load();
});

onMounted(load);
</script>

<template>
  <section class="page-header">
    <h1>{{ title }}</h1>
  </section>

  <section class="toolbar" aria-label="Resource filters">
    <input v-model="filters.keyword" placeholder="Keyword" @keyup.enter="load" />
    <input v-model="filters.cityId" placeholder="City ID" @keyup.enter="load" />
    <input v-model="filters.category" placeholder="Category" @keyup.enter="load" />
    <input v-model="filters.tag" placeholder="Tag" @keyup.enter="load" />
    <input v-model="filters.ratingMin" placeholder="Min rating" @keyup.enter="load" />
    <input v-model="filters.ratingMax" placeholder="Max rating" @keyup.enter="load" />
    <input v-model="filters.userId" placeholder="User ID" @keyup.enter="load" />
    <input v-model="filters.attractionId" placeholder="Attraction ID" @keyup.enter="load" />
    <input v-model="filters.targetId" placeholder="Target ID" @keyup.enter="load" />
    <input v-model="filters.targetType" placeholder="Target type" @keyup.enter="load" />
    <input v-model="filters.analysisType" placeholder="Analysis type" @keyup.enter="load" />
    <input v-model="filters.status" placeholder="Status" @keyup.enter="load" />
    <button type="button" @click="load">Search</button>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <section class="crud-layout">
    <div class="table-wrap">
      <div class="table-meta">{{ total }} records</div>
      <table>
        <thead>
          <tr>
            <th v-for="field in fields" :key="field">{{ field }}</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!loading && records.length === 0">
            <td :colspan="fields.length + 1">No records</td>
          </tr>
          <tr v-for="record in records" :key="record.id">
            <td v-for="field in fields" :key="field">{{ record[field] }}</td>
            <td class="actions">
              <button type="button" @click="edit(record)">Edit</button>
              <button v-if="isTravelNotes" type="button" @click="analyzeNote(record)">Analyze</button>
              <button v-if="canToggleStatus && 'status' in record" type="button" @click="toggleStatus(record)">Status</button>
              <button type="button" @click="remove(record)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <form class="editor-panel" @submit.prevent="save">
      <h2>{{ editingId ? 'Edit' : 'Create' }}</h2>
      <textarea v-model="formText" rows="16" spellcheck="false"></textarea>
      <div class="actions">
        <button type="submit">Save</button>
        <button type="button" @click="resetForm">Reset</button>
      </div>
    </form>
  </section>

  <section v-if="analysisResult" class="editor-panel" style="margin-top: 18px;">
    <h2>Analysis Result</h2>
    <pre>{{ JSON.stringify(analysisResult.data || analysisResult, null, 2) }}</pre>
  </section>
</template>
