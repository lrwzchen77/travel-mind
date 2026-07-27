<script setup>
import { computed, onMounted, ref } from 'vue';
import { ArrowLeft, ArrowRight, MapPin, Camera } from 'lucide-vue-next';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { journalApi, journalImageUrl } from '../api/journal.js';
import { useReveal } from '../composables/useReveal.js';

const root = ref(null);
useReveal(root);

const route = useRoute();
const router = useRouter();
const journal = ref(null);
const photos = ref([]);
const locations = ref([]);
const loading = ref(false);
const busy = ref('');
const error = ref('');
const message = ref('');
const uploadInput = ref(null);
const editOpen = ref(false);
const editForm = ref({ title: '', summary: '', coverImage: '' });

const heroPhoto = computed(() => photos.value[0]);
const locationCount = computed(() => locations.value.length);

function dayGroups(items) {
  const groups = new Map();
  for (const item of items) {
    const day = Number(item.dayIndex) > 0 ? Number(item.dayIndex) : 0;
    if (!groups.has(day)) groups.set(day, []);
    groups.get(day).push(item);
  }
  return [...groups].sort(([a], [b]) => a - b);
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const data = await journalApi.detail(route.params.id);
    journal.value = data.journal || {};
    photos.value = data.photos || [];
    locations.value = data.locations || [];
    editForm.value = {
      title: journal.value.title || '',
      summary: journal.value.summary || '',
      coverImage: journal.value.coverImage || '',
    };
  } catch (err) {
    error.value = err?.message || '暂时打不开这篇游记。';
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
  busy.value = 'upload';
  error.value = '';
  message.value = '';
  let added = 0;
  let uploadError = '';
  try {
    for (const file of files) {
      await journalApi.uploadAndAddPhoto(route.params.id, file);
      added++;
    }
  } catch (err) {
    uploadError = added ? `已保存 ${added} 张照片，其余上传失败。` : (err?.message || '照片上传失败。');
  } finally {
    busy.value = '';
  }
  await load();
  if (!error.value && uploadError) error.value = uploadError;
  if (added) message.value = `已添加 ${added} 张照片`;
}

async function removePhoto(photoId) {
  if (!window.confirm('删除这张照片？')) return;
  busy.value = `photo-${photoId}`;
  try {
    await journalApi.removePhoto(route.params.id, photoId);
    await load();
    message.value = '照片已删除';
  } catch (err) {
    error.value = err?.message || '删除失败。';
  } finally {
    busy.value = '';
  }
}

async function saveEdit() {
  busy.value = 'edit';
  error.value = '';
  try {
    await journalApi.update(route.params.id, editForm.value);
    await load();
    editOpen.value = false;
    message.value = '游记已更新';
  } catch (err) {
    error.value = err?.message || '更新失败。';
  } finally {
    busy.value = '';
  }
}

async function doPublish() {
  if (!window.confirm('发布这篇游记？发布后将标记为公开状态。')) return;
  busy.value = 'publish';
  try {
    await journalApi.publish(route.params.id);
    await load();
    message.value = '游记已发布';
  } catch (err) {
    error.value = err?.message || '发布失败。';
  } finally {
    busy.value = '';
  }
}

async function removeJournal() {
  if (!window.confirm('删除这篇游记？此操作无法恢复。')) return;
  busy.value = 'remove';
  try {
    await journalApi.remove(route.params.id);
    router.push('/journals');
  } catch (err) {
    error.value = err?.message || '删除失败。';
  } finally {
    busy.value = '';
  }
}

onMounted(load);
</script>

<template>
  <div ref="root" class="journal-detail-page">
    <RouterLink class="city-detail-back" to="/journals"><ArrowLeft :size="15" :stroke-width="2.2" /> 返回游记列表</RouterLink>
    <p v-if="error" class="error-line" role="alert">{{ error }}</p>
    <p v-if="message" class="success-line" role="status">{{ message }}</p>
    <div v-if="loading && !journal" class="empty-state">正在打开这篇游记…</div>

    <template v-if="journal">
      <section class="memory-hero">
        <div class="memory-hero-media">
          <img v-if="heroPhoto || journal.coverImage" :src="journalImageUrl(heroPhoto?.photoUrl || journal.coverImage)" :alt="journal.title || '旅行游记'" />
          <div v-else class="memory-hero-placeholder"><span>{{ journal.destinationCity || '旅行' }}</span><small>添加照片后，它会成为这篇游记的封面</small></div>
        </div>
        <div class="memory-hero-copy">
          <p class="memory-privacy">{{ journal.status === 'published' ? '公开' : '草稿 · 仅你可见' }}</p>
          <p class="memory-destination">{{ journal.destinationCity || '目的地待补充' }}</p>
          <h1>{{ journal.title || '未命名游记' }}</h1>
          <p>{{ journal.summary || '把照片和旅途片段按天整理，写成属于你的旅行故事。' }}</p>
          <time>{{ journal.travelDays || 1 }} 天行程 · {{ journal.viewCount || 0 }} 次浏览</time>
          <div class="memory-hero-actions">
            <button type="button" class="btn-coral" :disabled="Boolean(busy)" @click="uploadInput?.click()">{{ busy === 'upload' ? '正在添加…' : '添加照片' }}</button>
            <button type="button" class="btn-ghost" :disabled="Boolean(busy)" @click="editOpen = !editOpen">{{ editOpen ? '关闭编辑' : '编辑信息' }}</button>
            <button v-if="journal.status !== 'published'" type="button" class="btn-ghost" :disabled="busy === 'publish'" @click="doPublish">{{ busy === 'publish' ? '发布中…' : '发布游记' }}</button>
            <details class="memory-more">
              <summary>更多</summary>
              <button type="button" class="memory-delete" :disabled="busy === 'remove'" @click="removeJournal">{{ busy === 'remove' ? '删除中…' : '删除游记' }}</button>
            </details>
          </div>
          <input ref="uploadInput" class="memory-file-input" type="file" multiple accept="image/jpeg,image/png,image/webp" @change="uploadPhotos" />
        </div>
      </section>

      <section v-if="editOpen" class="glass-panel journal-editor">
        <p class="eyebrow">编辑游记</p>
        <div class="field-group">
          <label class="field-label">标题<input v-model.trim="editForm.title" maxlength="128" /></label>
          <label class="field-label">简介<textarea v-model.trim="editForm.summary" maxlength="1000" rows="3" /></label>
          <label class="field-label">封面图 URL<input v-model.trim="editForm.coverImage" maxlength="512" placeholder="上传照片后会自动填充" /></label>
        </div>
        <div class="journal-editor-actions">
          <button type="button" class="btn-coral" :disabled="busy === 'edit'" @click="saveEdit">{{ busy === 'edit' ? '保存中…' : '保存' }}</button>
          <button type="button" class="btn-ghost" @click="editOpen = false">取消</button>
        </div>
      </section>

      <section class="memory-timeline" aria-labelledby="journal-timeline-title">
        <div class="section-head"><div><p class="eyebrow">按日回看</p><h2 id="journal-timeline-title">游记时间线</h2></div></div>

        <div v-if="!photos.length && !locations.length" class="empty-state empty-state--card">
          <strong>游记还是空的</strong>
          <p>先添加照片，或者编辑游记信息来丰富内容。</p>
        </div>

        <section v-for="[day, dayPhotos] in dayGroups(photos)" :key="`photo-${day}`" class="memory-day">
          <header class="memory-date-stamp">
            <strong>第 {{ day || 1 }} 天</strong>
            <span>{{ dayPhotos.length }} 张照片</span>
          </header>
          <div class="memory-day-items">
            <article v-for="photo in dayPhotos" :key="photo.id" class="memory-item">
              <div class="memory-item-meta"><span><Camera :size="14" /> 旅行照片</span></div>
              <img :src="journalImageUrl(photo.photoUrl)" :alt="photo.caption || '旅行照片'" loading="lazy" />
              <div class="memory-item-copy">
                <p>{{ photo.caption || '这张照片还没有说明。' }}</p>
                <p v-if="photo.location" class="journal-location">{{ photo.location }}</p>
              </div>
              <button type="button" class="memory-photo-remove" :disabled="busy === `photo-${photo.id}`" @click="removePhoto(photo.id)">{{ busy === `photo-${photo.id}` ? '删除中…' : '删除照片' }}</button>
            </article>
          </div>
        </section>

        <section v-if="locations.length" class="memory-day">
          <header class="memory-date-stamp">
            <strong>地点足迹</strong>
            <span>{{ locationCount }} 个地点</span>
          </header>
          <div class="memory-day-items">
            <article v-for="loc in locations" :key="loc.id" class="memory-item">
              <div class="memory-item-meta"><span><MapPin :size="14" /> {{ loc.placeType || '地点' }}</span></div>
              <div class="memory-item-copy">
                <h3>{{ loc.placeName || '未命名地点' }}</h3>
                <p>{{ loc.description || loc.address || '' }}</p>
                <p v-if="loc.latitude && loc.longitude" class="journal-location">{{ loc.latitude }}, {{ loc.longitude }}</p>
              </div>
            </article>
          </div>
        </section>
      </section>
    </template>

    <section class="chapter-bridge" data-reveal>
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章 · 01 规划</p>
        <h2 class="chapter-bridge-title">写完游记，再去一次</h2>
        <p class="chapter-bridge-lead">每一段记录都是下一次出发的种子。带着这次的经验，打开规划器，开始新的路线。</p>
      </div>
      <RouterLink class="chapter-bridge-cta" to="/planning">
        <span>规划新行程</span>
        <ArrowRight :size="18" :stroke-width="2.2" />
      </RouterLink>
    </section>
  </div>
</template>

<style scoped>
.journal-editor { max-width: 920px; margin: 24px auto; }
.journal-editor-actions { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 16px; }
.journal-location { color: var(--tm-accent); font: 500 12px/1.5 var(--font-mono); }
</style>
