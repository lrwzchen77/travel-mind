<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ArrowRight } from 'lucide-vue-next';
import { RouterLink } from 'vue-router';
import { communityApi } from '../api/community.js';
import PagePrologue from '../components/PagePrologue.vue';
import PrivateImage from '../components/PrivateImage.vue';
import { cityImageByName } from '../data/cityImages.js';

const posts = ref([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const error = ref('');
const editingId = ref(null);
const editForm = reactive({ title: '', content: '', topic: 'route', tags: '' });
const topicLabel = { food: '吃什么', stay: '住哪里', play: '去哪玩', route: '路线', tip: '避坑' };

const publishedCount = computed(() => posts.value.filter((post) => post.visibility === 'public' && Number(post.status) === 1).length);
const pendingCount = computed(() => posts.value.filter((post) => post.visibility === 'public' && Number(post.status) === 0).length);
const rejectedCount = computed(() => posts.value.filter((post) => post.visibility === 'public' && ![0, 1].includes(Number(post.status))).length);
const hasMore = computed(() => posts.value.length < total.value);

function status(post) {
  if (post.visibility !== 'public') return { label: '仅自己可见', hint: '这篇内容没有投递到旅行社区。', class: 'is-private' };
  if (Number(post.status) === 1) return { label: '已发布', hint: '旅行者现在可以搜索和引用这篇分享。', class: 'is-published' };
  if (Number(post.status) === 0) return { label: '审核中', hint: '审核通过后会出现在旅行社区。', class: 'is-pending' };
  return { label: '未通过', hint: post.review_reason || '请修改内容后重新提交审核。', class: 'is-rejected' };
}

function cover(post) {
  return post.cover_image || cityImageByName[post.city] || '';
}

function excerpt(post) {
  const text = String(post.content || '');
  return text.length > 100 ? `${text.slice(0, 100)}…` : text;
}

function startEdit(post) {
  editingId.value = post.id;
  Object.assign(editForm, { title: post.title || '', content: post.content || '', topic: post.topic || 'route', tags: post.tags || '' });
}

async function saveEdit() {
  try {
    await communityApi.updatePost(editingId.value, editForm);
    editingId.value = null;
    await load();
  } catch (err) {
    error.value = err?.message || '修改失败。';
  }
}

async function submit(post) {
  try {
    await communityApi.submitPost(post.id);
    await load();
  } catch (err) {
    error.value = err?.message || '提交审核失败。';
  }
}

async function load(pageNum = 1) {
  loading.value = true;
  error.value = '';
  try {
    const data = await communityApi.myPosts({ pageNum, pageSize: 30 });
    posts.value = pageNum === 1 ? (data.records || []) : [...posts.value, ...(data.records || [])];
    total.value = data.total || posts.value.length;
    page.value = pageNum;
  } catch (err) {
    error.value = err?.message || '暂时打不开你的分享。';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <PagePrologue index="08" eyebrow="我的分享" title="你留下的旅行经验" lead="看看你发过的旅行分享，哪些被社区收藏，哪些还在审核。" next-label="发布新分享" next-to="/inspirations" />

  <section class="my-post-summary glass-panel">
    <div><strong>{{ total }}</strong><span>篇全部分享</span></div>
    <div><strong>{{ publishedCount }}</strong><span>篇{{ hasMore ? '当前已加载的' : '' }}已发布</span></div>
    <div><strong>{{ pendingCount }}</strong><span>篇{{ hasMore ? '当前已加载的' : '' }}审核中</span></div>
    <div v-if="rejectedCount"><strong>{{ rejectedCount }}</strong><span>篇{{ hasMore ? '当前已加载的' : '' }}未通过</span></div>
    <RouterLink class="btn-link btn-coral" to="/inspirations">发布新的分享</RouterLink>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>
  <div v-if="!loading && !posts.length" class="empty-state empty-state--card">
    <strong>还没有公开或私藏的旅行分享</strong>
    <p>一顿好饭、一次少走弯路的路线，都能帮到下一位旅行者。</p>
    <RouterLink class="btn-link btn-coral" to="/inspirations">去写第一篇</RouterLink>
  </div>

  <div v-else class="my-post-grid">
    <article v-for="post in posts" :key="post.id" class="my-post-card">
      <PrivateImage v-if="cover(post)" :src="cover(post)" :alt="post.title" loading="lazy" />
      <div class="my-post-card-body">
        <div class="my-post-meta"><span>{{ topicLabel[post.topic] || '旅行分享' }}</span><em :class="status(post).class">{{ status(post).label }}</em></div>
        <RouterLink v-if="status(post).class === 'is-published'" :to="`/inspirations/${post.id}`"><h2>{{ post.title }}</h2></RouterLink>
        <h2 v-else>{{ post.title }}</h2>
        <p>{{ post.city || '目的地待补充' }} · {{ excerpt(post) }}</p>
        <small>{{ status(post).hint }}</small>
        <div v-if="editingId !== post.id" class="actions">
          <button type="button" class="btn-ghost" @click="startEdit(post)">编辑</button>
          <button v-if="post.visibility !== 'public'" type="button" class="btn-coral" @click="submit(post)">提交审核</button>
        </div>
        <form v-else class="field-stack" @submit.prevent="saveEdit">
          <input v-model="editForm.title" maxlength="128" required aria-label="标题" />
          <select v-model="editForm.topic" aria-label="类型"><option v-for="(label, value) in topicLabel" :key="value" :value="value">{{ label }}</option></select>
          <input v-model="editForm.tags" maxlength="255" placeholder="标签" aria-label="标签" />
          <textarea v-model="editForm.content" maxlength="8000" rows="6" required aria-label="内容" />
          <div class="actions"><button type="submit" class="btn-coral">{{ post.visibility === 'public' ? '保存并重新审核' : '保存' }}</button><button type="button" class="btn-ghost" @click="editingId = null">取消</button></div>
        </form>
      </div>
    </article>
  </div>
  <div v-if="hasMore" class="load-more"><button type="button" class="btn-ghost" :disabled="loading" @click="load(page + 1)">{{ loading ? '正在加载…' : `加载更多（还有 ${total - posts.length} 篇）` }}</button></div>

  <section class="chapter-bridge" data-reveal>
    <div class="chapter-bridge-copy">
      <p class="chapter-bridge-eyebrow">下一章 · 03 社区</p>
      <h2 class="chapter-bridge-title">看看别人怎么写</h2>
      <p class="chapter-bridge-lead">自己的分享写完了，去社区翻翻别人的真实行程，常能找到下一程的灵感。</p>
    </div>
    <RouterLink class="chapter-bridge-cta" to="/inspirations">
      <span>去旅行社区</span>
      <ArrowRight :size="18" :stroke-width="2.2" />
    </RouterLink>
  </section>
</template>
