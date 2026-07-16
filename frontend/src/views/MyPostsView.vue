<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { communityApi } from '../api/community.js';
import { cityImageByName } from '../data/cityImages.js';

const posts = ref([]);
const loading = ref(false);
const error = ref('');
const topicLabel = { food: '吃什么', stay: '住哪里', play: '去哪玩', route: '路线', tip: '避坑' };

const publishedCount = computed(() => posts.value.filter((post) => post.visibility === 'public' && Number(post.status) === 1).length);
const pendingCount = computed(() => posts.value.filter((post) => post.visibility === 'public' && Number(post.status) === 0).length);

function status(post) {
  if (post.visibility !== 'public') return { label: '仅自己可见', hint: '这篇内容没有投递到旅行社区。', class: 'is-private' };
  if (Number(post.status) === 1) return { label: '已发布', hint: '旅行者现在可以搜索和引用这篇分享。', class: 'is-published' };
  return { label: '审核中', hint: '审核通过后会出现在旅行社区。', class: 'is-pending' };
}

function cover(post) {
  return post.cover_image || cityImageByName[post.city] || '';
}

function excerpt(post) {
  const text = String(post.content || '');
  return text.length > 100 ? `${text.slice(0, 100)}…` : text;
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const data = await communityApi.myPosts({ pageSize: 30 });
    posts.value = data.records || [];
  } catch (err) {
    error.value = err?.message || '暂时打不开你的分享。';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-intro">
    <p class="eyebrow">我的分享</p>
    <h1>你留下的旅行经验</h1>
    <p class="lead">每一篇都能在这里看到是否已公开给其他旅行者。</p>
  </section>

  <section class="my-post-summary glass-panel">
    <div><strong>{{ posts.length }}</strong><span>篇全部分享</span></div>
    <div><strong>{{ publishedCount }}</strong><span>篇已发布</span></div>
    <div><strong>{{ pendingCount }}</strong><span>篇审核中</span></div>
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
      <img v-if="cover(post)" :src="cover(post)" :alt="post.title" loading="lazy" />
      <div class="my-post-card-body">
        <div class="my-post-meta"><span>{{ topicLabel[post.topic] || '旅行分享' }}</span><em :class="status(post).class">{{ status(post).label }}</em></div>
        <RouterLink v-if="status(post).class === 'is-published'" :to="`/inspirations/${post.id}`"><h2>{{ post.title }}</h2></RouterLink>
        <h2 v-else>{{ post.title }}</h2>
        <p>{{ post.city || '目的地待补充' }} · {{ excerpt(post) }}</p>
        <small>{{ status(post).hint }}</small>
      </div>
    </article>
  </div>
</template>
