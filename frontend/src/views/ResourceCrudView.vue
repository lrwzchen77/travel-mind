<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import { resourceApi } from '../api/resources.js';
import { adminAiApi as aiApi } from '../api/ai.js';
import {
  ArrowRight,
  CircleCheck,
  Pencil,
  Power,
  Route as RouteIcon,
  Rows3,
  SearchX,
  Sparkles,
  Trash2,
  XCircle,
} from 'lucide-vue-next';
import { communityApi } from '../api/community.js';
import { useReveal } from '../composables/useReveal.js';
import PagePrologue from '../components/PagePrologue.vue';

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
const canDelete = computed(() => route.meta.canDelete !== false);
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
  if (value === 0 || value === '0') return isTravelNotes.value ? '待审核' : '下线';
  if (value === 2 || value === '2') return '已驳回';
  return value;
}

async function reviewNote(record, status) {
  const reason = status === 2 ? window.prompt('请填写驳回原因（用户可见）') : '';
  if (status === 2 && !reason?.trim()) return;
  try {
    await communityApi.reviewPost(record.id, status, reason);
    await load();
  } catch (err) {
    error.value = err?.message || '审核失败';
  }
}

async function resetUserPassword(record) {
  const password = window.prompt(`为账号 ${record.username} 设置新密码（至少 10 位）`);
  if (!password) return;
  try { await resourceApi.resetPassword(record.id, password); } catch (err) { error.value = err?.message || '密码重置失败'; }
}

async function changeUserRole(record) {
  const role = window.prompt('输入角色：user 或 admin', 'user');
  if (!['user', 'admin'].includes(role)) return;
  try { await resourceApi.updateRole(record.id, role); } catch (err) { error.value = err?.message || '角色修改失败'; }
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
  if (resourceKey.value === 'users') {
    formText.value = JSON.stringify({ username: '', nickname: '', phone: '', email: '', password: '', role: 'user' }, null, 2);
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
  <section v-if="isAdmin" class="admin-hero admin-hero--slim" data-reveal>
    <PagePrologue
      index="A2 · 资源"
      eyebrow="Resource Management"
    >
      <template #title><em>{{ title }}</em></template>
      <template #lead>维护平台内容资产：检索、审核、上下线、编辑与补充数据，确保前台展示信息完整可用。</template>
    </PagePrologue>
  </section>
  <section v-else class="page-intro">
    <p class="eyebrow">{{ isDiscover ? '发现' : '我的' }}</p>
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

  <div v-if="isAdmin" class="admin-section-head" data-reveal>
    <div>
      <p class="eyebrow">数据筛选</p>
      <h2>检索与操作</h2>
    </div>
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

  <div v-if="isAdmin" class="admin-section-head" data-reveal>
    <div>
      <p class="eyebrow">记录列表</p>
      <h2>{{ title }}</h2>
    </div>
  </div>

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
                <SearchX v-if="isAdmin" :size="32" aria-hidden="true" />
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
                v-if="canToggleStatus && !isTravelNotes && 'status' in record"
                type="button"
                class="table-icon-button"
                :aria-label="Number(record.status) === 1 ? '下线' : '上线'"
                :title="Number(record.status) === 1 ? '下线' : '上线'"
                @click="toggleStatus(record)"
              ><Power :size="16" aria-hidden="true" /></button>
              <button v-if="isTravelNotes && record.visibility === 'public' && Number(record.status) !== 1" type="button" class="table-icon-button is-primary" aria-label="审核通过" title="审核通过" @click="reviewNote(record, 1)"><CircleCheck :size="16" aria-hidden="true" /></button>
              <button v-if="isTravelNotes && record.visibility === 'public' && Number(record.status) !== 2" type="button" class="table-icon-button is-danger" aria-label="驳回" title="驳回并填写原因" @click="reviewNote(record, 2)"><XCircle :size="16" aria-hidden="true" /></button>
              <button v-if="resourceKey === 'users'" type="button" class="table-icon-button" aria-label="重置密码" title="重置密码" @click="resetUserPassword(record)">密</button>
              <button v-if="resourceKey === 'users'" type="button" class="table-icon-button" aria-label="修改角色" title="修改角色" @click="changeUserRole(record)">权</button>
              <button v-if="canDelete" type="button" class="table-icon-button is-danger" aria-label="删除" title="删除" @click="remove(record)"><Trash2 :size="16" aria-hidden="true" /></button>
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

  <section v-if="isAdmin" class="chapter-bridge" data-reveal>
    <div class="chapter-bridge-copy">
      <p class="chapter-bridge-eyebrow">下一章</p>
      <h2 class="chapter-bridge-title">运行配置</h2>
      <p class="chapter-bridge-lead">校验地图、内容采集与大模型上游凭证，确保所有服务链路稳定在线。</p>
    </div>
    <RouterLink class="chapter-bridge-cta" to="/admin/settings">
      <span>去运行配置</span>
      <ArrowRight :size="18" :stroke-width="2.2" aria-hidden="true" />
    </RouterLink>
  </section>
  </div>
</template>
