<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { ArrowLeft, ArrowRight } from 'lucide-vue-next';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { memoryApi, memoryImageUrl } from '../api/memory.js';
import { useReveal } from '../composables/useReveal.js';

const root = ref(null);
useReveal(root);

const route = useRoute();
const router = useRouter();
const memory = ref(null);
const loading = ref(false);
const busy = ref('');
const error = ref('');
const message = ref('');
const updateIssue = ref('');
const uploadInput = ref(null);
const question = ref('');
const answer = ref(null);
const focusedItem = ref('');
const askOpen = ref(false);
const shareOpen = ref(false);
const share = reactive({ title: '', note: '', tags: '', photo_item_id: '', confirmed: false });
const suggestions = ['我们第二天去了哪里？', '这趟旅行住在哪里？', '哪项体验花费最多？', '哪些照片能看出旅行地点？'];

const photos = computed(() => (memory.value?.items || []).filter((item) => item.item_type === 'photo'));
const heroPhoto = computed(() => photos.value[0]);
const timeline = computed(() => {
  const groups = new Map();
  for (const item of memory.value?.items || []) {
    const day = Number(item.day_index) > 0 ? Number(item.day_index) : 0;
    if (!groups.has(day)) groups.set(day, []);
    groups.get(day).push(item);
  }
  return [...groups].sort(([a], [b]) => a - b);
});
const dateRange = computed(() => {
  const dates = (memory.value?.items || []).map((item) => datePart(item.taken_at)).filter(Boolean).sort();
  if (!dates.length) return '日期待确认';
  return dates[0] === dates.at(-1) ? readableDate(dates[0]) : `${readableDate(dates[0])} — ${readableDate(dates.at(-1))}`;
});
const needsUpdate = computed(() => memory.value?.generation_status === 'pending');
const analyzeFailed = computed(() => updateIssue.value === 'analyze' || memory.value?.generation_status === 'failed');
const findUnavailable = computed(() => updateIssue.value === 'index' || ['failed', 'unavailable'].includes(memory.value?.index_status));
const publicPreview = computed(() => {
  const lines = ['来自真实行程 · 已由旅行者确认公开'];
  if (share.note.trim()) lines.push(share.note.trim());
  for (const item of (memory.value?.items || []).filter((entry) => ['place', 'photo'].includes(entry.item_type)).slice(0, 8)) {
    const fact = item.place_name || (item.item_type === 'photo' ? item.ai_caption : '');
    if (fact) lines.push(`${item.day_index ? `第 ${item.day_index} 天` : '旅行片段'} · ${fact}`);
  }
  return lines;
});

function parseArray(value) {
  if (Array.isArray(value)) return value;
  try { return JSON.parse(value || '[]'); } catch { return []; }
}

function idKey(value) {
  const id = String(value ?? '').trim();
  return /^\d+$/.test(id) ? id.replace(/^0+(?=\d)/, '') : id;
}

function tags(item) {
  return parseArray(item.ai_tags).filter(Boolean);
}

function itemLabel(item) {
  return { trip_summary: '行程概览', place: '到过的地方', expense: '旅途消费', photo: '旅行照片' }[item.item_type] || '旅途片段';
}

function itemText(item) {
  if (item.item_type === 'photo') return item.ai_caption || '这张照片还没有说明。';
  return item.content || item.place_name || '已记录';
}

function datePart(value) {
  return String(value || '').match(/^(\d{4}-\d{2}-\d{2})/)?.[1] || '';
}

function readableDate(value) {
  const match = String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})/);
  return match ? `${match[1]}年${Number(match[2])}月${Number(match[3])}日` : '';
}

function dayStamp(day, items) {
  const date = items.map((item) => datePart(item.taken_at)).find(Boolean);
  const placeItem = items.find((item) => ['place', 'photo'].includes(item.item_type) && (item.place_name || item.city));
  return {
    date: date ? `${Number(date.slice(5, 7))}月${Number(date.slice(8, 10))}日` : (day ? `第${day}天` : '旅行概览'),
    day: date && day ? `第${day}天` : (!date ? '时间待确认' : '日期已确认'),
    place: placeItem?.place_name || placeItem?.city || memory.value?.destination_city || '地点待确认',
  };
}

async function load() {
  loading.value = true;
  try {
    memory.value = await memoryApi.detail(route.params.id);
    if (!share.title) share.title = memory.value.title || `${memory.value.destination_city || ''}旅行记录`;
  } catch (err) {
    error.value = err?.message || '暂时打不开这篇旅行记录。';
  } finally {
    loading.value = false;
  }
}

async function updateRecord() {
  busy.value = 'update';
  updateIssue.value = '';
  error.value = '';
  message.value = '';
  try {
    try {
      await memoryApi.analyze(route.params.id);
    } catch {
      updateIssue.value = 'analyze';
      await load();
      return false;
    }
    try {
      await memoryApi.index(route.params.id);
    } catch {
      updateIssue.value = 'index';
      await load();
      return false;
    }
    await load();
    message.value = '记录已更新';
    return true;
  } finally {
    busy.value = '';
  }
}

async function retryFind() {
  busy.value = 'retry-find';
  updateIssue.value = '';
  error.value = '';
  try {
    await memoryApi.index(route.params.id);
    await load();
    message.value = '现在可以查找旅行细节了';
  } catch {
    updateIssue.value = 'index';
    await load();
  } finally {
    busy.value = '';
  }
}

async function uploadPhotos(event) {
  const files = [...(event.target.files || [])];
  event.target.value = '';
  if (!files.length) return;
  const invalid = files.find((file) => !['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 8 * 1024 * 1024);
  if (invalid) { error.value = '每张照片都必须是不超过 8MB 的 JPG、PNG 或 WebP。'; return; }
  busy.value = 'upload';
  error.value = '';
  message.value = '';
  let added = 0;
  let partialError = '';
  try {
    added = (await memoryApi.addPhotos(route.params.id, files)).length;
  } catch (err) {
    added = Number(err?.addedCount || 0);
    partialError = added
      ? `已保存 ${added} 张照片，其余照片没有加入，可以稍后重试。`
      : (err?.message || '照片没有加入旅行记录，请检查后重试。');
  }
  await load();
  busy.value = '';
  if (added) await updateRecord();
  if (partialError) error.value = partialError;
}

async function ask() {
  if (!question.value.trim()) return;
  busy.value = 'ask';
  error.value = '';
  answer.value = null;
  try { answer.value = await memoryApi.ask(route.params.id, question.value.trim()); }
  catch (err) { error.value = err?.message || '这次没有找到旅行细节，请稍后重试。'; }
  finally { busy.value = ''; }
}

async function askSuggestion(value) {
  question.value = value;
  await ask();
}

async function focusCitation(citation) {
  focusedItem.value = idKey(citation.memoryItemId);
  await nextTick();
  const target = document.getElementById(`memory-item-${focusedItem.value}`);
  target?.scrollIntoView?.({ behavior: globalThis.matchMedia?.('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'center' });
  target?.focus({ preventScroll: true });
}

async function removePhoto(item) {
  if (!window.confirm('从旅行记录中删除这张照片？其他内容会保留。')) return;
  busy.value = `photo-${item.id}`;
  error.value = '';
  try {
    await memoryApi.removeItem(route.params.id, item.id);
    await load();
    busy.value = '';
    await updateRecord();
  } catch (err) {
    error.value = err?.message || '照片删除失败。';
  } finally {
    busy.value = '';
  }
}

async function removeMemory() {
  if (!window.confirm('删除这篇旅行记录？记录中的照片关联和时间线会一并删除，但“我的行程”仍会保留。此操作无法恢复。')) return;
  busy.value = 'remove';
  error.value = '';
  try { await memoryApi.remove(route.params.id); router.push('/memories'); }
  catch (err) { error.value = err?.message || '旅行记录删除失败。'; }
  finally { busy.value = ''; }
}

async function publish() {
  if (!share.confirmed || !window.confirm('确认把这份公开内容提交到旅行社区？')) return;
  busy.value = 'publish';
  error.value = '';
  message.value = '';
  try {
    const payload = { title: share.title, note: share.note, tags: share.tags };
    if (share.photo_item_id) payload.photo_item_id = idKey(share.photo_item_id);
    await memoryApi.publish(route.params.id, payload);
    message.value = '已提交社区审核；审核通过后，其他旅行者才能看到公开内容。';
    shareOpen.value = false;
  } catch (err) {
    error.value = err?.message || '这次没有分享成功，请检查内容后重试。';
  } finally {
    busy.value = '';
  }
}

onMounted(async () => {
  error.value = '';
  await load();
  if (memory.value && route.query?.update === '1') {
    const { update, ...query } = route.query;
    await router.replace({ path: route.path, query });
    await updateRecord();
  }
});
</script>

<template>
  <div ref="root">
  <RouterLink class="city-detail-back" to="/memories"><ArrowLeft :size="15" :stroke-width="2.2" /> 返回旅行记录</RouterLink>
  <p v-if="error" class="error-line" role="alert">{{ error }}</p>
  <p v-if="message" class="success-line" role="status">{{ message }}</p>
  <div v-if="loading && !memory" class="empty-state">正在打开这篇旅行记录…</div>

  <template v-if="memory">
    <section class="memory-hero">
      <div class="memory-hero-media">
        <img v-if="heroPhoto" :src="memoryImageUrl(heroPhoto.source_url)" :alt="heroPhoto.ai_caption || `${memory.destination_city || '旅行'}照片`" />
        <div v-else class="memory-hero-placeholder"><span>{{ memory.destination_city || '旅行' }}</span><small>添加照片后，它会成为这篇记录的封面</small></div>
      </div>
      <div class="memory-hero-copy">
        <p class="memory-privacy">私密 · 仅你可见</p>
        <p class="memory-destination">{{ memory.destination_city || '目的地待补充' }}</p>
        <h1>{{ memory.title || '未命名旅行记录' }}</h1>
        <p>{{ memory.summary || '照片、地点和旅途片段会在这里按天放在一起。' }}</p>
        <time>{{ dateRange }}</time>
        <div class="memory-hero-actions">
          <button type="button" class="btn-coral" :disabled="Boolean(busy)" @click="uploadInput?.click()">{{ busy === 'upload' ? '正在添加…' : (photos.length ? '继续添加照片' : '添加照片') }}</button>
          <button type="button" class="btn-ghost" :aria-expanded="askOpen" aria-controls="memory-ask-panel" @click="askOpen = !askOpen">找旅行细节</button>
          <button type="button" class="btn-ghost" :aria-expanded="shareOpen" aria-controls="memory-share-panel" @click="shareOpen = !shareOpen">分享这趟旅行</button>
          <details class="memory-more">
            <summary>更多</summary>
            <button type="button" class="memory-delete" :disabled="busy === 'remove'" @click="removeMemory">{{ busy === 'remove' ? '删除中…' : '删除旅行记录' }}</button>
          </details>
        </div>
        <input ref="uploadInput" class="memory-file-input" type="file" multiple accept="image/jpeg,image/png,image/webp" @change="uploadPhotos" />
      </div>
    </section>

    <section v-if="busy === 'update'" class="memory-update-state" aria-live="polite"><strong>正在整理这趟旅行…</strong><span>正在按照片时间、地点和行程把内容排好，请留在当前页面。</span></section>
    <section v-else-if="analyzeFailed" class="memory-update-state is-error" role="alert"><strong>照片已保存，但暂时没能整理</strong><button type="button" class="text-link" :disabled="Boolean(busy)" @click="updateRecord">重新整理</button></section>
    <section v-else-if="needsUpdate" class="memory-update-state"><strong>这本记录有新内容</strong><button type="button" class="text-link" :disabled="Boolean(busy)" @click="updateRecord">更新记录</button></section>

    <section class="memory-timeline" aria-labelledby="memory-timeline-title">
      <div class="section-head"><div><p class="eyebrow">按日回看</p><h2 id="memory-timeline-title">旅行时间线</h2></div></div>
      <div v-if="!timeline.length" class="empty-state empty-state--card"><strong>时间线还是空的</strong><p>先添加照片，或者从“我的行程”记录这趟旅行。</p></div>
      <section v-for="[day, items] in timeline" :key="day" class="memory-day">
        <header class="memory-date-stamp">
          <strong>{{ dayStamp(day, items).date }}</strong>
          <span>{{ dayStamp(day, items).day }}</span>
          <small>{{ dayStamp(day, items).place }}</small>
        </header>
        <div class="memory-day-items">
          <article v-for="item in items" :id="`memory-item-${idKey(item.id)}`" :key="item.id" class="memory-item" :class="{ 'is-focused': focusedItem === idKey(item.id) }" tabindex="-1">
            <div class="memory-item-meta"><span>{{ itemLabel(item) }}</span><time>{{ readableDate(datePart(item.taken_at)) || (day ? `第 ${day} 天` : '时间待确认') }}</time></div>
            <img v-if="item.item_type === 'photo'" :src="memoryImageUrl(item.source_url)" :alt="item.ai_caption || '旅行照片'" loading="lazy" />
            <div class="memory-item-copy"><h3>{{ item.place_name || itemLabel(item) }}</h3><p>{{ itemText(item) }}</p><div v-if="tags(item).length" class="chip-row"><span v-for="tag in tags(item)" :key="tag" class="chip">{{ tag }}</span></div><small v-if="item.item_type === 'expense'">这项消费只有你能看到</small></div>
            <button v-if="item.item_type === 'photo'" type="button" class="memory-photo-remove" :disabled="busy === `photo-${item.id}`" @click="removePhoto(item)">{{ busy === `photo-${item.id}` ? '删除中…' : '删除照片' }}</button>
          </article>
        </div>
      </section>
    </section>

    <section v-if="askOpen" id="memory-ask-panel" class="memory-ask glass-panel" aria-labelledby="memory-ask-title">
      <p class="eyebrow">只查这趟旅行</p><h2 id="memory-ask-title">找旅行细节</h2>
      <p>只从这趟旅行的记录里找，不会混入其他行程。</p>
      <div v-if="findUnavailable" class="memory-find-notice" role="alert"><span>记录已更新，暂时不能查找旅行细节。</span><button type="button" class="text-link" :disabled="Boolean(busy)" @click="retryFind">{{ busy === 'retry-find' ? '正在重试…' : '重试' }}</button></div>
      <div v-else-if="memory.generation_status === 'ready' && memory.index_status === 'pending'" class="memory-find-notice"><span>记录内容已更新，准备好后就能查找旅行细节。</span><button type="button" class="text-link" :disabled="Boolean(busy)" @click="retryFind">准备查找</button></div>
      <div v-else-if="memory.generation_status === 'pending'" class="memory-find-notice"><span>这本记录有新内容，更新后就能查找细节。</span><button type="button" class="text-link" :disabled="Boolean(busy)" @click="updateRecord">更新记录</button></div>
      <div class="memory-suggestions" aria-label="问题示例"><button v-for="suggestion in suggestions" :key="suggestion" type="button" :disabled="Boolean(busy) || memory.index_status !== 'ready'" @click="askSuggestion(suggestion)">{{ suggestion }}</button></div>
      <form @submit.prevent="ask"><label for="memory-question" class="field-label">想找什么</label><textarea id="memory-question" v-model="question" maxlength="500" rows="3" placeholder="例如：我们第二天去了哪里？" /><button class="btn-coral" type="submit" :disabled="busy === 'ask' || memory.index_status !== 'ready' || !question.trim()">{{ busy === 'ask' ? '正在找…' : '查找' }}</button></form>
      <div v-if="answer" class="memory-answer" :class="{ 'is-empty': !answer.citations?.length }"><span>{{ answer.citations?.length ? '相关记录' : '没有找到相关内容' }}</span><p>{{ answer.citations?.length ? answer.answer : '这本记录里没有找到相关内容。换个问法，或添加照片后再试。' }}</p><small v-if="answer.citations?.length">回答只根据这趟旅行中已有的内容整理。</small><div v-if="answer.citations?.length" class="memory-citations"><button v-for="(citation, index) in answer.citations" :key="`${citation.memoryItemId}-${index}`" type="button" @click="focusCitation(citation)"><b>相关记录 {{ index + 1 }}</b><span>{{ citation.excerpt }}</span><em>查看这条记录 <ArrowRight :size="15" :stroke-width="2.2" /></em></button></div></div>
    </section>

    <section v-if="shareOpen" id="memory-share-panel" class="memory-share glass-panel" aria-labelledby="memory-share-title">
      <div class="memory-share-form">
        <p class="eyebrow">分享这趟旅行</p><h2 id="memory-share-title">确认要分享的内容</h2>
        <p>这是公开预览。只有你确认的内容会提交到旅行社区。</p>
        <div class="memory-share-scope"><div><strong>会公开</strong><ul><li>公开标题和旅行感受</li><li>记录中的地点和旅行片段</li><li>你选择的 0 或 1 张封面</li><li>“来自真实行程”标识</li></ul></div><div><strong>不会公开</strong><ul><li>其他照片</li><li>精确位置和照片定位信息</li><li>消费明细</li><li>你问过的问题和答案</li><li>这篇私密旅行记录</li></ul></div></div>
        <label><span class="field-label">公开标题</span><input v-model.trim="share.title" maxlength="128" required /></label>
        <label><span class="field-label">一句旅行感受（选填）</span><textarea v-model.trim="share.note" maxlength="600" rows="3" placeholder="写下愿意公开的旅行感受" /></label>
        <label><span class="field-label">公开标签（选填）</span><input v-model.trim="share.tags" maxlength="200" placeholder="例如：湖景、慢游" /></label>
        <fieldset class="memory-cover-choice"><legend>公开封面（0 或 1 张）</legend><label><input v-model="share.photo_item_id" type="radio" value="" />不发布照片</label><label v-for="photo in photos" :key="photo.id"><input v-model="share.photo_item_id" type="radio" :value="idKey(photo.id)" /><img :src="memoryImageUrl(photo.source_url)" :alt="photo.ai_caption || '旅行照片'" /></label></fieldset>
        <label class="memory-confirm"><input v-model="share.confirmed" type="checkbox" />我已检查公开预览，并确认只提交以上内容。</label>
        <button type="button" class="btn-coral" :disabled="busy === 'publish' || !share.confirmed || !share.title" @click="publish">{{ busy === 'publish' ? '正在提交…' : '提交社区审核' }}</button>
      </div>
      <aside class="memory-share-preview"><span>公开预览</span><img v-if="share.photo_item_id" :src="memoryImageUrl(photos.find((photo) => idKey(photo.id) === share.photo_item_id)?.source_url)" alt="所选公开封面预览" /><h3>{{ share.title || memory.title }}</h3><p v-for="line in publicPreview" :key="line">{{ line }}</p><small>#真实行程 {{ share.tags }}</small></aside>
    </section>
  </template>

    <section class="chapter-bridge" data-reveal>
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章 · 01 规划</p>
        <h2 class="chapter-bridge-title">回忆写完，再去一次</h2>
        <p class="chapter-bridge-lead">每一段记录都是下一次出发的种子。带着这次的经验，打开规划器，开始新的路线。</p>
      </div>
      <RouterLink class="chapter-bridge-cta" to="/planning">
        <span>规划新行程</span>
        <ArrowRight :size="18" :stroke-width="2.2" />
      </RouterLink>
    </section>
  </div>
</template>
