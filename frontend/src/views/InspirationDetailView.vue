<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { authSession } from '../auth/session.js';
import { communityApi } from '../api/community.js';
import { cityImageByName } from '../data/cityImages.js';

const route = useRoute();
const router = useRouter();
const post = ref(null);
const intent = ref('reference');
const loading = ref(false);
const error = ref('');
const message = ref('');
const interactionError = ref('');
const comments = ref([]);
const commentsLoading = ref(false);
const comment = ref('');
const submitting = ref(false);
const topicLabel = { food: '吃什么', stay: '住哪里', play: '去哪玩', route: '路线', tip: '避坑' };
const cover = computed(() => post.value?.cover_image || cityImageByName[post.value?.city] || '');

async function load() {
  loading.value = true;
  try { post.value = await communityApi.post(route.params.id); await loadComments(); } catch (err) { error.value = err?.message || '这篇社区分享暂时不可见。'; } finally { loading.value = false; }
}

async function loadComments() {
  commentsLoading.value = true;
  try {
    const data = await communityApi.comments(route.params.id, { pageSize: 50 });
    comments.value = data.records || [];
    if (post.value) post.value.comment_count = data.total ?? comments.value.length;
  }
  catch (err) { interactionError.value = err?.message || '评论暂时没有加载出来。'; }
  finally { commentsLoading.value = false; }
}

function login() {
  router.push({ path: '/login', query: { redirect: route.fullPath } });
}

async function toggleLike() {
  if (!authSession.isLoggedIn()) { login(); return; }
  interactionError.value = '';
  try { Object.assign(post.value, await communityApi[post.value.liked_by_me ? 'unlike' : 'like'](post.value.id)); }
  catch (err) { interactionError.value = err?.message || '点赞失败，请稍后再试。'; }
}

async function publishComment() {
  if (!authSession.isLoggedIn()) { login(); return; }
  submitting.value = true;
  interactionError.value = '';
  try {
    await communityApi.createComment(post.value.id, comment.value);
    comment.value = '';
    await loadComments();
  } catch (err) { interactionError.value = err?.message || '评论发布失败。'; }
  finally { submitting.value = false; }
}

async function removeComment(item) {
  interactionError.value = '';
  try {
    await communityApi.deleteComment(item.id);
    comments.value = comments.value.filter(({ id }) => id !== item.id);
    post.value.comment_count = Math.max(0, Number(post.value.comment_count || 0) - 1);
  } catch (err) { interactionError.value = err?.message || '评论删除失败。'; }
}

async function addToBag() {
  if (!authSession.isLoggedIn()) { router.push({ path: '/login', query: { redirect: route.fullPath } }); return; }
  try { await communityApi.addToBag(post.value.id, intent.value); message.value = '已加入灵感包，可以带去生成行程。'; } catch (err) { error.value = err?.message || '加入灵感包失败。'; }
}

onMounted(load);
</script>

<template>
  <RouterLink class="city-detail-back" to="/inspirations">← 返回旅行社区</RouterLink>
  <p v-if="error" class="error-line">{{ error }}</p>
  <article v-if="post" class="inspiration-detail">
    <img v-if="cover" class="inspiration-detail-cover" :src="cover" :alt="post.title" />
    <div class="inspiration-detail-copy"><p class="eyebrow">{{ topicLabel[post.topic] || '旅行分享' }} · {{ post.city || '目的地' }}</p><h1>{{ post.title }}</h1><p class="inspiration-author">{{ post.author || '旅行者' }} 分享 · {{ post.create_time || '近期' }}</p><div class="chip-row"><span v-for="tag in String(post.tags || '').split(/[,，、\s]+/).filter(Boolean)" :key="tag" class="chip">{{ tag }}</span></div><p class="inspiration-content">{{ post.content }}</p><section class="community-interactions" aria-labelledby="comments-title"><div class="community-interaction-head"><h2 id="comments-title">旅行者评论（{{ post.comment_count || 0 }}）</h2><button type="button" class="btn-ghost like-button" :aria-pressed="Boolean(post.liked_by_me)" @click="toggleLike">{{ post.liked_by_me ? '已赞' : '点赞' }} · {{ post.like_count || 0 }}</button></div><form class="comment-form" @submit.prevent="publishComment"><label for="comment">说点对后来旅行者有用的话</label><textarea id="comment" v-model="comment" maxlength="1000" rows="3" required placeholder="分享补充信息或真实体验…" /><button class="btn-coral" type="submit" :disabled="submitting">{{ submitting ? '发布中…' : '发布评论' }}</button></form><p v-if="interactionError" class="error-line" role="alert">{{ interactionError }}</p><p v-if="commentsLoading" class="empty-state">正在加载评论…</p><div v-else-if="!comments.length" class="empty-state empty-state--card">还没有评论，来分享第一条有用信息吧。</div><ol v-else class="comment-list"><li v-for="item in comments" :key="item.id"><div><strong>{{ item.author || '旅行者' }}</strong><time>{{ item.create_time || '刚刚' }}</time></div><p>{{ item.content }}</p><button v-if="item.is_mine" type="button" class="text-link" :aria-label="`删除 ${item.author || '我的'} 的评论`" @click="removeComment(item)">删除</button></li></ol></section></div>
  <aside class="inspiration-action glass-panel"><p class="eyebrow">引用到这趟旅行</p><h2>把这份体验带进行程</h2><label class="field-label" for="intent">怎么参考它</label><select id="intent" v-model="intent"><option value="must">必须安排</option><option value="priority">优先参考</option><option value="reference">仅作参考 / 避坑</option></select><button type="button" class="btn-coral" @click="addToBag">加入灵感包</button><RouterLink class="btn-link btn-ghost" :to="{ path: '/assistant', query: { inspirationIds: post.id } }">先问 AI 怎么取舍</RouterLink><p v-if="message" class="success-line">{{ message }}</p></aside>
  </article>
  <div v-else-if="loading" class="empty-state">正在打开社区分享…</div>
</template>
