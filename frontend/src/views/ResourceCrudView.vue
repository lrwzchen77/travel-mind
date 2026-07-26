<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import { resourceApi } from '../api/resources.js';
import { adminAiApi as aiApi } from '../api/ai.js';
import { Pencil, Power, Route as RouteIcon, Rows3, Sparkles, Trash2 } from 'lucide-vue-next';
import { useReveal } from '../composables/useReveal.js';

const root = ref(null);
useReveal(root);

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
const isPoiResource = computed(() => ['attractions', 'hotels', 'restaurants', 'map-pois'].includes(resourceKey.value));
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
  if (isPoiResource.value) {
    const payload = {
      city: '', name: '', longitude: 0, latitude: 0, category: '', rating: null, cost: null, tags: '', imageUrl: '',
    };
    if (resourceKey.value === 'map-pois') payload.kind = 'attraction';
    formText.value = JSON.stringify(payload, null, 2);
    return;
  }
  formText.value = '{\n  "name": ""\n}';
}

function cleanFilters() {
  return Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== '' && value !== null));
}

function planThisCity(record) {
  const city = record.name || record.city || '';
  if (city) {
    window.location.href = `/map?city=${encodeURIComponent(city)}`;
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
  <div ref="root" class="resource-page" :class="{ 'resource-page--admin': isAdmin }">
  <section class="page-intro">
    <p class="eyebrow">{{ isAdmin ? '运营管理' : (isDiscover ? '发现' : '我的') }}</p>
    <h1>{{ title }}</h1>
  </section>

  <div v-if="isDiscover" class="actions discovery-shortcuts">
    <RouterLink class="btn-link btn-coral" to="/map">
      带着灵感去规划
    </RouterLink>
    <RouterLink class="btn-link btn-ghost" to="/cities">城市</RouterLink>
    <RouterLink class="btn-link btn-ghost" to="/attractions">景点</RouterLink>
    <RouterLink class="btn-link btn-ghost" to="/hotels">住哪里</RouterLink>
    <RouterLink class="btn-link btn-ghost" to="/restaurants">吃什么</RouterLink>
  </div>

  <section class="toolbar" aria-label="搜索筛选" data-reveal>
    <input v-model="filters.keyword" placeholder="搜名称或关键词" @keyup.enter="load" />
    <input v-model="filters.cityId" placeholder="城市编号" @keyup.enter="load" />
    <input v-model="filters.category" placeholder="分类" @keyup.enter="load" />
    <input v-model="filters.tag" placeholder="标签" @keyup.enter="load" />
    <input v-model="filters.status" placeholder="状态" @keyup.enter="load" />
    <button type="button" class="btn-coral" @click="load">搜索</button>
    <button type="button" class="btn-ghost" @click="showEditor = true; resetForm()">{{ isPoiResource ? '手动导入' : '添加' }}</button>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <section class="explore-layout" data-reveal>
    <div class="soft-table-wrap">
      <div class="table-meta">
        <span><Rows3 :size="15" aria-hidden="true" />{{ loading ? '加载中…' : `${total} 条记录` }}</span>
      </div>
      <table :aria-label="`${title}列表`">
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
            <td v-for="field in fields" :key="field" :class="`resource-cell--${field}`">
              <span
                v-if="field === 'status'"
                class="resource-status"
                :class="{ 'is-active': record[field] === 1 || record[field] === '1' }"
              ><i aria-hidden="true" />{{ cellValue(record, field) }}</span>
              <span v-else>{{ cellValue(record, field) }}</span>
            </td>
            <td class="table-actions">
              <button
                v-if="!isAdmin && resourceKey === 'cities' && record.name"
                type="button"
                class="table-icon-button is-primary"
                :aria-label="`规划${record.name}`"
                :title="`规划${record.name}`"
                @click="planThisCity(record)"
              ><RouteIcon :size="16" aria-hidden="true" /></button>
              <button type="button" class="table-icon-button" aria-label="编辑" title="编辑" @click="edit(record)"><Pencil :size="16" aria-hidden="true" /></button>
              <button
                v-if="isTravelNotes"
                type="button"
                class="table-icon-button"
                aria-label="分析"
                title="分析"
                @click="analyzeNote(record)"
              ><Sparkles :size="16" aria-hidden="true" /></button>
              <button
                v-if="canToggleStatus && 'status' in record"
                type="button"
                class="table-icon-button"
                :aria-label="Number(record.status) === 1 ? '下线' : '上线'"
                :title="Number(record.status) === 1 ? '下线' : '上线'"
                @click="toggleStatus(record)"
              ><Power :size="16" aria-hidden="true" /></button>
              <button type="button" class="table-icon-button is-danger" aria-label="删除" title="删除" @click="remove(record)"><Trash2 :size="16" aria-hidden="true" /></button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <form v-if="showEditor" class="editor-panel field-stack" @submit.prevent="save">
      <h2 class="panel-title">{{ editingId ? '改一改' : (isPoiResource ? '手动导入补充数据' : '添加一条') }}</h2>
      <textarea v-model="formText" class="code-area" rows="14" spellcheck="false" />
      <div class="actions">
        <button type="submit" class="btn-coral">保存</button>
        <button type="button" class="btn-ghost" @click="showEditor = false; resetForm()">取消</button>
      </div>
    </form>
  </section>

  <section v-if="analysisResult" class="glass-panel analysis-result">
    <h2>分析结果</h2>
    <pre>{{ JSON.stringify(analysisResult.data || analysisResult, null, 2) }}</pre>
  </section>
  </div>
</template>

<style scoped>
/* ── Admin mode: cinematic operations table ── */
.resource-page--admin .page-intro h1 {
  font-family: var(--font-display);
  font-size: clamp(28px, 4vw, 40px);
  letter-spacing: -0.035em;
  line-height: 1.08;
  color: var(--tm-ink);
}
.resource-page--admin .page-intro .eyebrow {
  color: var(--tm-accent);
}

/* ── Toolbar: responsive grid + editorial focus ── */
.resource-page--admin .toolbar {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  padding: 18px;
  border: 1px solid var(--tm-line);
  border-radius: var(--tm-radius-panel);
  background: var(--tm-paper-muted);
  position: relative;
}
.resource-page--admin .toolbar::before {
  content: "";
  position: absolute;
  top: 0;
  left: 18px;
  width: 36px;
  height: 2px;
  background: var(--tm-accent);
  transform: translateY(-1px);
}
.resource-page--admin .toolbar input,
.resource-page--admin .toolbar select {
  min-width: 0;
  padding: 10px 14px;
  border: 1px solid var(--tm-line);
  border-radius: var(--tm-radius-control);
  background: var(--tm-paper);
  color: var(--tm-ink);
  font-size: 13px;
  font-family: var(--font-mono);
  letter-spacing: 0.02em;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}
.resource-page--admin .toolbar input::placeholder { color: var(--tm-muted); }
.resource-page--admin .toolbar input:focus,
.resource-page--admin .toolbar select:focus {
  border-color: var(--tm-accent);
  box-shadow: 0 0 0 3px var(--tm-accent-soft);
  outline: none;
}
.resource-page--admin .toolbar .btn-coral,
.resource-page--admin .toolbar .btn-ghost {
  padding: 10px 18px;
  border-radius: var(--tm-radius-pill);
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.02em;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s ease;
}
.resource-page--admin .toolbar .btn-coral {
  background: linear-gradient(135deg, var(--tm-accent) 0%, var(--tm-accent-deep) 100%);
  color: #160d05;
  box-shadow: 0 8px 22px -10px var(--tm-accent-glow);
  border: 0;
}
.resource-page--admin .toolbar .btn-coral:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px -10px var(--tm-accent-glow);
}
.resource-page--admin .toolbar .btn-ghost {
  border: 1px solid var(--tm-line-strong);
  background: transparent;
  color: var(--tm-ink);
}
.resource-page--admin .toolbar .btn-ghost:hover {
  border-color: var(--tm-accent);
  color: var(--tm-accent);
  background: var(--tm-accent-soft);
}

/* ── Table wrapper: editorial depth ── */
.resource-page--admin .soft-table-wrap {
  border: 1px solid var(--tm-line);
  border-radius: var(--tm-radius-panel);
  background: linear-gradient(180deg, var(--tm-paper-muted) 0%, var(--tm-paper) 100%);
  overflow: hidden;
}
.resource-page--admin .table-meta {
  padding: 12px 18px;
  border-bottom: 1px solid var(--tm-line-soft);
  background: var(--tm-accent-soft);
}
.resource-page--admin .table-meta > span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--tm-accent) !important;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}
.resource-page--admin .table-meta :deep(svg),
.resource-page--admin .table-meta svg { color: var(--tm-accent); }

/* ── Table rows: editorial hover ── */
.resource-page--admin table { width: 100%; border-collapse: collapse; }
.resource-page--admin thead th {
  padding: 12px 16px;
  text-align: left;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--tm-muted);
  border-bottom: 1px solid var(--tm-line);
  background: var(--tm-accent-soft);
}
.resource-page--admin tbody td {
  padding: 13px 16px;
  border-bottom: 1px solid var(--tm-line-soft);
  color: var(--tm-ink-soft);
  font-size: 13.5px;
  transition: background 0.2s ease, color 0.2s ease;
}
.resource-page--admin tbody tr { transition: background 0.2s ease; }
.resource-page--admin tbody tr:hover { background: var(--tm-accent-soft); }
.resource-page--admin tbody tr:hover td { color: var(--tm-ink); }
.resource-page--admin tbody tr:last-child td { border-bottom: 0; }

/* ── Status badge: accent for active ── */
.resource-page--admin .resource-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 9px;
  border-radius: var(--tm-radius-pill);
  font-family: var(--font-mono);
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  border: 1px solid var(--tm-line-strong);
  color: var(--tm-muted);
}
.resource-page--admin .resource-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--tm-muted);
}
.resource-page--admin .resource-status.is-active {
  border-color: var(--tm-accent-soft);
  color: var(--tm-accent);
  background: var(--tm-accent-soft);
}
.resource-page--admin .resource-status.is-active i {
  background: var(--tm-accent);
  box-shadow: 0 0 8px var(--tm-accent-glow);
}

/* ── Action buttons: editorial icon grid ── */
.resource-page--admin .table-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: flex-end;
}
.resource-page--admin .table-icon-button {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid var(--tm-line-strong);
  border-radius: var(--tm-radius-control);
  background: var(--tm-paper);
  color: var(--tm-ink-soft);
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
.resource-page--admin .table-icon-button:hover {
  transform: translateY(-2px);
  border-color: var(--tm-accent);
  color: var(--tm-accent);
  background: var(--tm-accent-soft);
  box-shadow: 0 6px 16px -8px var(--tm-accent-glow);
}
.resource-page--admin .table-icon-button.is-primary {
  border-color: var(--tm-accent-soft);
  color: var(--tm-accent);
}
.resource-page--admin .table-icon-button.is-danger:hover {
  border-color: var(--tm-danger);
  color: var(--tm-danger);
  background: var(--tm-danger-soft);
  box-shadow: 0 6px 16px -8px rgba(216, 60, 60, 0.4);
}

/* ── Empty state: editorial placeholder ── */
.resource-page--admin .empty-state {
  display: grid;
  gap: 8px;
  padding: 40px 20px;
  text-align: center;
  color: var(--tm-muted);
  font-size: 13px;
}
.resource-page--admin .empty-state strong {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 700;
  color: var(--tm-ink);
  letter-spacing: -0.01em;
}

/* ── Editor panel: cinematic form ── */
.resource-page--admin .editor-panel {
  border: 1px solid var(--tm-line);
  border-radius: var(--tm-radius-panel);
  background: var(--tm-paper-muted);
  padding: 22px;
  position: relative;
}
.resource-page--admin .editor-panel::before {
  content: "";
  position: absolute;
  top: 0;
  left: 22px;
  width: 40px;
  height: 2px;
  background: var(--tm-accent);
  transform: translateY(-1px);
}
.resource-page--admin .panel-title {
  margin: 0 0 16px;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.015em;
  color: var(--tm-ink);
}
.resource-page--admin .code-area {
  width: 100%;
  padding: 14px;
  border: 1px solid var(--tm-line);
  border-radius: var(--tm-radius-control);
  background: var(--tm-canvas);
  color: var(--tm-ink);
  font-family: var(--font-mono);
  font-size: 12.5px;
  line-height: 1.7;
  resize: vertical;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}
.resource-page--admin .code-area:focus {
  border-color: var(--tm-accent);
  box-shadow: 0 0 0 3px var(--tm-accent-soft);
  outline: none;
}
</style>
