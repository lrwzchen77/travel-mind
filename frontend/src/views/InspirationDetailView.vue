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
const topicLabel = { food: '吃什么', stay: '住哪里', play: '去哪玩', route: '路线', tip: '避坑' };
const cover = computed(() => post.value?.cover_image || cityImageByName[post.value?.city] || '');

async function load() {
  loading.value = true;
  try { post.value = await communityApi.post(route.params.id); } catch (err) { error.value = err?.message || '这篇社区分享暂时不可见。'; } finally { loading.value = false; }
}

async function addToBag() {
  if (!authSession.isLoggedIn()) { router.push({ path: '/login', query: { redirect: route.fullPath } }); return; }
  try { await communityApi.addToBag(post.value.id, intent.value); message.value = '已加入灵感包，可以随时拿给 AI 伴游规划。'; } catch (err) { error.value = err?.message || '加入灵感包失败。'; }
}

onMounted(load);
</script>

<template>
  <RouterLink class="city-detail-back" to="/inspirations">← 返回旅行社区</RouterLink>
  <p v-if="error" class="error-line">{{ error }}</p>
  <article v-if="post" class="inspiration-detail">
    <img v-if="cover" class="inspiration-detail-cover" :src="cover" :alt="post.title" />
    <div class="inspiration-detail-copy"><p class="eyebrow">{{ topicLabel[post.topic] || '旅行分享' }} · {{ post.city || '目的地' }}</p><h1>{{ post.title }}</h1><p class="inspiration-author">{{ post.author || '旅行者' }} 分享 · {{ post.create_time || '近期' }}</p><div class="chip-row"><span v-for="tag in String(post.tags || '').split(/[,，、\s]+/).filter(Boolean)" :key="tag" class="chip">{{ tag }}</span></div><p class="inspiration-content">{{ post.content }}</p></div>
  <aside class="inspiration-action glass-panel"><p class="eyebrow">引用到这趟旅行</p><h2>把这份体验交给 AI</h2><label class="field-label" for="intent">怎么参考它</label><select id="intent" v-model="intent"><option value="must">必须安排</option><option value="priority">优先参考</option><option value="reference">仅作参考 / 避坑</option></select><button type="button" class="btn-coral" @click="addToBag">加入灵感包</button><RouterLink class="btn-link btn-ghost" :to="{ path: '/assistant', query: { inspirationIds: post.id } }">先问 AI 伴游</RouterLink><p v-if="message" class="success-line">{{ message }}</p></aside>
  </article>
  <div v-else-if="loading" class="empty-state">正在打开社区分享…</div>
</template>
