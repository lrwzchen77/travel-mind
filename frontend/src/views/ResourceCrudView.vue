<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import { resourceApi } from '../api/resources.js';
import { adminAiApi as aiApi } from '../api/ai.js';

const route = useRoute();
const loading = ref(false);
const error = ref('');
const records = ref([]);
const total = ref(0);
const editingId = ref(null);
const formText = ref('{\n  "name": ""\n}');
const analysisResult = ref(null);
const showEditor = ref(false);
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
const fieldLabels = computed(() => route.meta.fieldLabels || {});
const canToggleStatus = computed(() => route.meta.canToggleStatus !== false);
const isTravelNotes = computed(() => resourceKey.value === 'travel-notes');
const isAdmin = computed(() => route.meta.admin === true);
const isDiscover = computed(() =>
  !isAdmin.value && ['cities', 'attractions', 'hotels', 'restaurants'].includes(resourceKey.value),
);

function labelOf(field) {
  return fieldLabels.value[field] || field;
}

function statusText(value) {
  if (value === 1 || value === '1') return '开放';
  if (value === 0 || value === '0') return '下线';
  return value;
}

function cellValue(record, field) {
  const value = record[field];
  if (field === 'status') return statusText(value);
  return value ?? '—';
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const data = await resourceApi.list(resourceKey.value, cleanFilters());
    records.value = data.records || [];
    total.value = data.total || 0;
  } catch (err) {
    error.value = err?.message || '加载失败，稍后再试';
  } finally {
    loading.value = false;
  }
}

async function save() {
  error.value = '';
  try {
    const payload = JSON.parse(formText.value);
    if (editingId.value) {
      await resourceApi.update(resourceKey.value, editingId.value, payload);
    } else {
      await resourceApi.create(resourceKey.value, payload);
    }
    resetForm();
    showEditor.value = false;
    await load();
  } catch (err) {
    error.value = err?.message || '保存失败';
  }
}

async function remove(record) {
  if (!window.confirm('确定删除这条内容吗？')) return;
  try {
    await resourceApi.remove(resourceKey.value, record.id);
    await load();
  } catch (err) {
    error.value = err?.message || '删除失败';
  }
}

async function toggleStatus(record) {
  try {
    await resourceApi.updateStatus(resourceKey.value, record.id, Number(record.status) === 1 ? 0 : 1);
    await load();
  } catch (err) {
    error.value = err?.message || '更新失败';
  }
}

async function analyzeNote(record) {
  error.value = '';
  try {
    analysisResult.value = await aiApi.analyzeContent(
      {
        text: record.content || record.title || '',
        city: String(record.city_id || ''),
        attraction_name: String(record.attraction_id || ''),
        language: 'zh',
      },
      { targetId: record.id, targetType: 'travel_note' },
    );
  } catch (err) {
    error.value = err?.message || '分析失败';
  }
}

function edit(record) {
  editingId.value = record.id;
  formText.value = JSON.stringify(record, null, 2);
  showEditor.value = true;
}

function resetForm() {
  editingId.value = null;
  formText.value = '{\n  "name": ""\n}';
}

function cleanFilters() {
  return Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== '' && value !== null));
}

function planThisCity(record) {
  const city = record.name || record.city || '';
  if (city) {
    window.location.href = `/planning?city=${encodeURIComponent(city)}`;
  }
}

watch(() => route.fullPath, () => {
  resetForm();
  analysisResult.value = null;
  showEditor.value = false;
  load();
});

onMounted(load);
</script>

<template>
  <section class="page-intro">
    <p class="eyebrow">{{ isAdmin ? '运营管理' : (isDiscover ? '发现' : '我的') }}</p>
    <h1>{{ title }}</h1>
  </section>

  <div v-if="isDiscover" class="actions" style="margin-bottom: 18px;">
    <RouterLink class="btn-link btn-coral" style="min-height: 40px; font-size: 13px; padding: 0 16px;" to="/planning">
      带着灵感去规划
    </RouterLink>
    <RouterLink class="btn-link btn-ghost" style="min-height: 40px; font-size: 13px; padding: 0 16px;" to="/cities">城市</RouterLink>
    <RouterLink class="btn-link btn-ghost" style="min-height: 40px; font-size: 13px; padding: 0 16px;" to="/attractions">景点</RouterLink>
    <RouterLink class="btn-link btn-ghost" style="min-height: 40px; font-size: 13px; padding: 0 16px;" to="/hotels">住哪里</RouterLink>
    <RouterLink class="btn-link btn-ghost" style="min-height: 40px; font-size: 13px; padding: 0 16px;" to="/restaurants">吃什么</RouterLink>
  </div>

  <section class="toolbar" aria-label="搜索筛选">
    <input v-model="filters.keyword" placeholder="搜名称或关键词" @keyup.enter="load" />
    <input v-model="filters.cityId" placeholder="城市编号" @keyup.enter="load" />
    <input v-model="filters.category" placeholder="分类" @keyup.enter="load" />
    <input v-model="filters.tag" placeholder="标签" @keyup.enter="load" />
    <input v-model="filters.status" placeholder="状态" @keyup.enter="load" />
    <button type="button" class="btn-coral" @click="load">搜索</button>
    <button type="button" class="btn-ghost" @click="showEditor = true; resetForm()">添加</button>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <section class="explore-layout">
    <div class="soft-table-wrap">
      <div class="table-meta">
        <span>{{ loading ? '加载中…' : `找到 ${total} 条` }}</span>
      </div>
      <table>
        <thead>
          <tr>
            <th v-for="field in fields" :key="field">{{ labelOf(field) }}</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!loading && records.length === 0">
            <td :colspan="fields.length + 1">
              <div class="empty-state">
                <strong>这里空空的</strong>
                换个关键词，或添加一条你感兴趣的内容。
              </div>
            </td>
          </tr>
          <tr v-for="record in records" :key="record.id">
            <td v-for="field in fields" :key="field">{{ cellValue(record, field) }}</td>
            <td class="actions">
              <button
                v-if="!isAdmin && resourceKey === 'cities' && record.name"
                type="button"
                class="btn-coral btn-sm"
                @click="planThisCity(record)"
              >去规划</button>
              <button type="button" class="btn-ghost btn-sm" @click="edit(record)">编辑</button>
              <button
                v-if="isTravelNotes"
                type="button"
                class="btn-ghost btn-sm"
                @click="analyzeNote(record)"
              >分析</button>
              <button
                v-if="canToggleStatus && 'status' in record"
                type="button"
                class="btn-ghost btn-sm"
                @click="toggleStatus(record)"
              >上下线</button>
              <button type="button" class="btn-danger btn-sm" @click="remove(record)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <form v-if="showEditor" class="editor-panel field-stack" @submit.prevent="save">
      <h2 class="panel-title">{{ editingId ? '改一改' : '添加一条' }}</h2>
      <textarea v-model="formText" class="code-area" rows="14" spellcheck="false" />
      <div class="actions">
        <button type="submit" class="btn-coral">保存</button>
        <button type="button" class="btn-ghost" @click="showEditor = false; resetForm()">取消</button>
      </div>
    </form>
  </section>

  <section v-if="analysisResult" class="glass-panel" style="margin-top: 20px;">
    <h2>分析结果</h2>
    <pre>{{ JSON.stringify(analysisResult.data || analysisResult, null, 2) }}</pre>
  </section>
</template>
