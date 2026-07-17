<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { memoryApi, memoryImageUrl } from '../api/memory.js';

const route = useRoute();
const router = useRouter();
const memory = ref(null);
const loading = ref(false);
const busy = ref('');
const error = ref('');
const message = ref('');
const uploadInput = ref(null);
const question = ref('这次旅行去了哪里？');
const answer = ref(null);
const focusedItem = ref('');
const shareOpen = ref(false);
const share = reactive({ title: '', note: '', tags: '', photo_item_id: '', confirmed: false });

const photos = computed(() => (memory.value?.items || []).filter((item) => item.item_type === 'photo'));
const latestTimeline = computed(() => (memory.value?.generations || []).find((item) => item.generation_type === 'timeline'));
const evidenceIds = computed(() => new Set(parseArray(latestTimeline.value?.evidence_json).map(idKey)));
const timeline = computed(() => {
  const groups = new Map();
  for (const item of memory.value?.items || []) {
    const day = Number(item.day_index) > 0 ? Number(item.day_index) : 0;
    if (!groups.has(day)) groups.set(day, []);
    groups.get(day).push(item);
  }
  return [...groups].sort(([a], [b]) => (a || 999) - (b || 999));
});
const publicPreview = computed(() => {
  const lines = ['来自真实行程 · 已由旅行者确认公开'];
  if (share.note.trim()) lines.push(share.note.trim());
  for (const item of (memory.value?.items || []).filter((entry) => ['place', 'photo'].includes(entry.item_type)).slice(0, 8)) {
    const fact = item.place_name || (item.item_type === 'photo' ? item.ai_caption : '');
    if (fact) lines.push(`${item.day_index ? `Day ${item.day_index}` : '旅行片段'} · ${fact}`);
  }
  return lines;
});

const generationState = computed(() => ({
  pending: ['待整理', '上传照片后运行 AI 整理，生成可追溯的按日时间线。'],
  processing: ['正在整理', '正在读取照片时间与场景，请稍候。'],
  ready: ['时间线已生成', `第 ${latestTimeline.value?.version || 1} 版，所有事实仍可回到证据。`],
  failed: ['整理失败', '保留了原始项目，可以重新运行整理。'],
}[memory.value?.generation_status] || ['等待整理', '']));
const indexState = computed(() => ({
  pending: ['待建立索引', '整理有变化后需要重新建立问答索引。'],
  indexing: ['正在建立索引', '正在把这一本记忆变成可检索知识。'],
  ready: ['问答已就绪', '问题只会检索这一本记忆，并返回引用。'],
  unavailable: ['问答暂不可用', '向量服务没有响应，可以稍后重试。'],
  failed: ['索引失败', '可以重新建立索引。'],
}[memory.value?.index_status] || ['待建立索引', '']));

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
  return { trip_summary: '行程摘要', place: '地点', expense: '真实支出', photo: '照片' }[item.item_type] || '旅行记录';
}

function itemText(item) {
  if (item.item_type === 'photo') return item.ai_caption || '这张照片还没有 AI 说明。';
  return item.content || item.place_name || '已记录';
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    memory.value = await memoryApi.detail(route.params.id);
    if (!share.title) share.title = memory.value.title || `${memory.value.destination_city || ''}旅行回忆`;
  } catch (err) {
    error.value = err?.message || '暂时打不开这本旅行记忆。';
  } finally {
    loading.value = false;
  }
}

async function uploadPhotos(event) {
  const files = [...(event.target.files || [])];
  event.target.value = '';
  if (!files.length) return;
  const invalid = files.find((file) => !['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 8 * 1024 * 1024);
  if (invalid) { error.value = '每张照片都必须是不超过 8MB 的 JPG、PNG 或 WebP。'; return; }
  busy.value = 'upload'; error.value = ''; message.value = '';
  try {
    await memoryApi.addPhotos(route.params.id, files);
    message.value = `已加入 ${files.length} 张照片。接下来运行 AI 整理。`;
    await load();
  } catch (err) {
    error.value = err?.message || '照片没有全部加入记忆册，请检查后重试。';
    await load();
  } finally { busy.value = ''; }
}

async function run(action, success) {
  busy.value = action; error.value = ''; message.value = '';
  try {
    await memoryApi[action](route.params.id);
    message.value = success;
    await load();
  } catch (err) {
    error.value = err?.message || `${action === 'analyze' ? 'AI 整理' : '建立索引'}失败，请稍后重试。`;
    await load();
  } finally { busy.value = ''; }
}

async function ask() {
  if (!question.value.trim()) return;
  busy.value = 'ask'; error.value = ''; answer.value = null;
  try { answer.value = await memoryApi.ask(route.params.id, question.value.trim()); }
  catch (err) { error.value = err?.message || '这次没有问成，请稍后重试。'; }
  finally { busy.value = ''; }
}

async function focusCitation(citation) {
  focusedItem.value = idKey(citation.memoryItemId);
  await nextTick();
  const target = document.getElementById(`memory-item-${focusedItem.value}`);
  target?.scrollIntoView?.({ behavior: globalThis.matchMedia?.('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'center' });
  target?.focus({ preventScroll: true });
}

async function removePhoto(item) {
  if (!window.confirm('从私有记忆册中删除这张照片？原上传文件不会公开。')) return;
  busy.value = `photo-${item.id}`; error.value = '';
  try { await memoryApi.removeItem(route.params.id, item.id); message.value = '照片已从记忆册删除，需要重新整理与索引。'; await load(); }
  catch (err) { error.value = err?.message || '照片删除失败。'; }
  finally { busy.value = ''; }
}

async function removeMemory() {
  if (!window.confirm('删除整本旅行记忆？时间线、照片关联和问答索引都会删除，且无法恢复。')) return;
  busy.value = 'remove'; error.value = '';
  try { await memoryApi.remove(route.params.id); router.push('/memories'); }
  catch (err) { error.value = err?.message || '记忆册删除失败。'; }
  finally { busy.value = ''; }
}

async function publish() {
  if (!share.confirmed || !window.confirm('确认发布脱敏副本到旅行社区？私有记忆册仍不会对外开放。')) return;
  busy.value = 'publish'; error.value = ''; message.value = '';
  try {
    const payload = { title: share.title, note: share.note, tags: share.tags };
    if (share.photo_item_id) payload.photo_item_id = idKey(share.photo_item_id);
    await memoryApi.publish(route.params.id, payload);
    message.value = '脱敏副本已提交审核；审核通过后，匿名旅行者只能看到这份公开内容。';
    shareOpen.value = false;
  } catch (err) { error.value = err?.message || '公开副本没有发布成功，请检查内容后重试。'; }
  finally { busy.value = ''; }
}

onMounted(load);
</script>

<template>
  <RouterLink class="city-detail-back" to="/memories">← 返回旅行记忆</RouterLink>
  <p v-if="error" class="error-line" role="alert">{{ error }}</p>
  <p v-if="message" class="success-line" role="status">{{ message }}</p>
  <div v-if="loading && !memory" class="empty-state">正在打开这本记忆…</div>

  <template v-if="memory">
    <section class="memory-hero">
      <div>
        <p class="eyebrow">仅自己可见 · {{ memory.destination_city || '旅行记忆' }}</p>
        <h1>{{ memory.title }}</h1>
        <p>{{ memory.summary || '照片、地点和旅行事实会在这里按天整理。' }}</p>
      </div>
      <div class="memory-hero-actions">
        <button type="button" class="btn-ghost" @click="shareOpen = !shareOpen">{{ shareOpen ? '收起发布预览' : '制作公开分享' }}</button>
        <button type="button" class="btn-danger" :disabled="busy === 'remove'" @click="removeMemory">{{ busy === 'remove' ? '删除中…' : '删除整册' }}</button>
      </div>
    </section>

    <section class="memory-control-strip" aria-label="旅行记忆处理状态">
      <article><span>01 · 照片</span><strong>{{ photos.length }} 张</strong><button type="button" class="text-link" :disabled="busy === 'upload'" @click="uploadInput?.click()">{{ busy === 'upload' ? '上传中…' : '继续添加' }}</button></article>
      <article><span>02 · AI 整理</span><strong>{{ generationState[0] }}</strong><small>{{ generationState[1] }}</small><button type="button" class="text-link" :disabled="Boolean(busy)" @click="run('analyze', 'AI 整理完成，可以查看按日证据。')">{{ busy === 'analyze' ? '整理中…' : '运行整理' }}</button></article>
      <article><span>03 · 知识索引</span><strong>{{ indexState[0] }}</strong><small>{{ indexState[1] }}</small><button type="button" class="text-link" :disabled="Boolean(busy)" @click="run('index', '知识索引已更新，现在可以提问。')">{{ busy === 'index' ? '索引中…' : '建立索引' }}</button></article>
      <input ref="uploadInput" class="memory-file-input" type="file" multiple accept="image/jpeg,image/png,image/webp" @change="uploadPhotos" />
    </section>

    <section v-if="shareOpen" class="memory-share glass-panel" aria-labelledby="memory-share-title">
      <div class="memory-share-form">
        <p class="eyebrow">脱敏公开副本</p><h2 id="memory-share-title">只分享你确认的内容</h2>
        <p>不会带出精确 GPS、消费明细、私有记忆编号或问答证据。照片可不选；选择时仅发布这一张去除元数据后的封面。</p>
        <label><span class="field-label">公开标题</span><input v-model.trim="share.title" maxlength="128" required /></label>
        <label><span class="field-label">一句旅行感受（选填）</span><textarea v-model.trim="share.note" maxlength="600" rows="3" placeholder="不要填写消费金额或精确位置" /></label>
        <label><span class="field-label">公开标签（选填）</span><input v-model.trim="share.tags" maxlength="200" placeholder="例如：湖景、慢游" /></label>
        <fieldset class="memory-cover-choice"><legend>公开封面（0 或 1 张）</legend><label><input v-model="share.photo_item_id" type="radio" value="" />不发布照片</label><label v-for="photo in photos" :key="photo.id"><input v-model="share.photo_item_id" type="radio" :value="idKey(photo.id)" /><img :src="memoryImageUrl(photo.source_url)" :alt="photo.ai_caption || '旅行照片'" /></label></fieldset>
        <label class="memory-confirm"><input v-model="share.confirmed" type="checkbox" />我已检查右侧预览，并确认发布脱敏副本；私有记忆册不会公开。</label>
        <button type="button" class="btn-coral" :disabled="busy === 'publish' || !share.confirmed || !share.title" @click="publish">{{ busy === 'publish' ? '发布中…' : '提交社区审核' }}</button>
      </div>
      <aside class="memory-share-preview"><span>公开预览</span><img v-if="share.photo_item_id" :src="memoryImageUrl(photos.find((photo) => idKey(photo.id) === share.photo_item_id)?.source_url)" alt="所选公开封面预览" /><h3>{{ share.title || memory.title }}</h3><p v-for="line in publicPreview" :key="line">{{ line }}</p><small>#真实行程 {{ share.tags }}</small></aside>
    </section>

    <div class="memory-workspace">
      <section class="memory-timeline" aria-labelledby="memory-timeline-title">
        <div class="section-head"><div><p class="eyebrow">旅行证据带</p><h2 id="memory-timeline-title">按天回看</h2></div><span>{{ evidenceIds.size }} 条已纳入时间线证据</span></div>
        <div v-if="!timeline.length" class="empty-state empty-state--card"><strong>时间线还是空的</strong><p>先添加照片，或重新从行程生成这本记忆。</p></div>
        <section v-for="[day, items] in timeline" :key="day" class="memory-day">
          <header><span>{{ day ? `DAY ${String(day).padStart(2, '0')}` : '未归日' }}</span><small>{{ items.length }} 条记录</small></header>
          <div class="memory-day-items">
            <article v-for="item in items" :id="`memory-item-${idKey(item.id)}`" :key="item.id" class="memory-item" :class="{ 'is-focused': focusedItem === idKey(item.id), 'is-evidence': evidenceIds.has(idKey(item.id)) }" tabindex="-1">
              <div class="memory-item-meta"><span>{{ itemLabel(item) }}</span><time>{{ item.taken_at || (day ? `第 ${day} 天` : '时间待确认') }}</time><em v-if="evidenceIds.has(idKey(item.id))">已纳入证据</em></div>
              <img v-if="item.item_type === 'photo'" :src="memoryImageUrl(item.source_url)" :alt="item.ai_caption || '旅行照片'" loading="lazy" />
              <div class="memory-item-copy"><h3>{{ item.place_name || itemLabel(item) }}</h3><p>{{ itemText(item) }}</p><div v-if="tags(item).length" class="chip-row"><span v-for="tag in tags(item)" :key="tag" class="chip">{{ tag }}</span></div><small v-if="item.item_type === 'expense'">消费明细仅在私有记忆册显示</small></div>
              <button v-if="item.item_type === 'photo'" type="button" class="memory-photo-remove" :disabled="busy === `photo-${item.id}`" @click="removePhoto(item)">{{ busy === `photo-${item.id}` ? '删除中…' : '删除照片' }}</button>
            </article>
          </div>
        </section>
      </section>

      <aside class="memory-ask glass-panel" aria-labelledby="memory-ask-title">
        <p class="eyebrow">只问这趟旅行</p><h2 id="memory-ask-title">从证据里找答案</h2>
        <p>{{ indexState[1] }}</p>
        <form @submit.prevent="ask"><label for="memory-question" class="field-label">你的问题</label><textarea id="memory-question" v-model="question" maxlength="500" rows="4" placeholder="例如：这次去了哪里？" /><button class="btn-coral" type="submit" :disabled="busy === 'ask' || memory.index_status !== 'ready'">{{ busy === 'ask' ? '检索中…' : '查找答案' }}</button></form>
        <div v-if="answer" class="memory-answer" :class="{ 'is-empty': !answer.citations?.length }"><span>{{ answer.citations?.length ? '基于以下旅行证据' : '没有找到证据' }}</span><p>{{ answer.answer }}</p><small v-if="answer.fallback && answer.citations?.length">这是检索摘要，没有添加证据之外的推测。</small><div v-if="answer.citations?.length" class="memory-citations"><button v-for="(citation, index) in answer.citations" :key="`${citation.memoryItemId}-${index}`" type="button" @click="focusCitation(citation)"><b>证据 {{ index + 1 }}</b><span>{{ citation.excerpt }}</span><em>定位到时间线 →</em></button></div><p v-else>可以换个问法，或补充照片后重新整理、建立索引。</p></div>
      </aside>
    </div>
  </template>
</template>
