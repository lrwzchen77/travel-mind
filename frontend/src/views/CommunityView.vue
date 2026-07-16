<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { authSession } from '../auth/session.js';
import { communityApi } from '../api/community.js';
import { cityImageByName } from '../data/cityImages.js';
import ImageDropUpload from '../components/ImageDropUpload.vue';

const route = useRoute();
const router = useRouter();
const posts = ref([]);
const total = ref(0);
const loading = ref(false);
const error = ref('');
const message = ref('');
const composing = ref(false);
const filters = reactive({ keyword: '', city: String(route.query.city || ''), topic: '' });
const form = reactive({ title: '', city: String(route.query.city || ''), topic: 'route', tags: '', cover_image: '', content: '', visibility: 'public' });

const topics = [
  ['food', '吃什么'], ['stay', '住哪里'], ['play', '去哪玩'], ['route', '路线'], ['tip', '避坑'],
];
const topicLabel = computed(() => Object.fromEntries(topics));

function cover(post) {
  return post.cover_image || cityImageByName[post.city] || '';
}

function excerpt(post) {
  const text = String(post.content || '');
  return text.length > 96 ? `${text.slice(0, 96)}…` : text;
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const data = await communityApi.posts({ ...filters, pageSize: 24 });
    posts.value = data.records || [];
    total.value = data.total || posts.value.length;
  } catch (err) {
    error.value = err?.message || '旅行社区暂时没有加载出来。';
  } finally {
    loading.value = false;
  }
}

function startPublish() {
  if (!authSession.isLoggedIn()) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  composing.value = !composing.value;
}

async function publish() {
  error.value = '';
  try {
    await communityApi.createPost(form);
    Object.assign(form, { title: '', city: filters.city, topic: 'route', tags: '', cover_image: '', content: '', visibility: 'public' });
    composing.value = false;
    message.value = '已提交发布；审核通过后会出现在旅行社区。';
    await load();
  } catch (err) {
    error.value = err?.message || '发布失败，请稍后再试。';
  }
}

onMounted(load);
</script>

<template>
  <section class="page-intro community-intro">
    <p class="eyebrow">旅行社区</p>
    <h1>每个人的真实旅行，值得被看见</h1>
    <div class="actions">
      <RouterLink class="btn-link btn-coral" to="/assistant">问 AI 伴游</RouterLink>
      <button type="button" class="btn-ghost" @click="startPublish">{{ composing ? '收起发布' : '发布我的分享' }}</button>
    </div>
  </section>

  <form class="community-filter glass-panel" @submit.prevent="load">
    <input v-model="filters.keyword" placeholder="搜城市、店名、路线或避坑关键词" />
    <input v-model="filters.city" placeholder="城市，例如杭州" />
    <select v-model="filters.topic">
      <option value="">全部类型</option>
      <option v-for="[value, label] in topics" :key="value" :value="value">{{ label }}</option>
    </select>
    <button class="btn-coral" type="submit" :disabled="loading">{{ loading ? '正在找…' : '找灵感' }}</button>
  </form>

  <form v-if="composing" class="glass-panel field-stack community-compose" @submit.prevent="publish">
    <div class="section-head" style="margin-top: 0; padding-top: 0; border: 0;">
      <div><p class="eyebrow">发布原创内容</p><h2>分享一段真正有用的旅行体验</h2></div>
    </div>
    <div class="field-row"><label><span class="field-label">标题</span><input v-model="form.title" required placeholder="例如：杭州两天一夜，住西湖边不踩坑" /></label><label><span class="field-label">城市</span><input v-model="form.city" placeholder="例如：杭州" /></label></div>
    <div class="field-row"><label><span class="field-label">类型</span><select v-model="form.topic"><option v-for="[value, label] in topics" :key="value" :value="value">{{ label }}</option></select></label><label><span class="field-label">标签</span><input v-model="form.tags" placeholder="亲子、少走路、美食" /></label></div>
    <ImageDropUpload v-model="form.cover_image" label="拖拽封面图到这里，或点击选择" />
    <label><span class="field-label">真实体验</span><textarea v-model="form.content" rows="7" required placeholder="写下花费、路线、时间、好吃不好吃，以及你希望后来的人避开的坑…" /></label>
    <label><span class="field-label">可见范围</span><select v-model="form.visibility"><option value="public">公开分享（审核后展示）</option><option value="private">只自己可见</option></select></label>
    <div class="actions"><button type="submit" class="btn-coral">发布到旅行社区</button><button type="button" class="btn-ghost" @click="composing = false">取消</button></div>
  </form>

  <p v-if="message" class="success-line">{{ message }}</p>
  <p v-if="error" class="error-line">{{ error }}</p>
  <div class="section-head"><div><h2>{{ loading ? '正在翻旅行分享…' : `${total} 篇旅行者分享` }}</h2></div><RouterLink class="text-link" to="/inspiration-bag">打开灵感包 →</RouterLink></div>

  <div v-if="!loading && !posts.length" class="empty-state empty-state--card"><strong>还没有匹配的灵感</strong><p>换个城市或关键词试试，也可以成为第一位分享的人。</p></div>
  <div v-else class="community-grid">
    <article v-for="post in posts" :key="post.id" class="community-card">
      <RouterLink :to="`/inspirations/${post.id}`" class="community-cover">
        <img v-if="cover(post)" :src="cover(post)" :alt="post.title" loading="lazy" />
        <span v-else>{{ post.city || '旅行社区' }}</span>
        <em>{{ topicLabel[post.topic] || '旅行分享' }}</em>
      </RouterLink>
      <div class="community-card-body"><p class="community-card-meta">{{ post.city || '目的地待补充' }} · {{ post.author || '旅行者' }}</p><RouterLink :to="`/inspirations/${post.id}`"><h2>{{ post.title }}</h2></RouterLink><p>{{ excerpt(post) }}</p><div class="chip-row"><span v-for="tag in String(post.tags || '').split(/[,，、\s]+/).filter(Boolean).slice(0, 3)" :key="tag" class="chip">{{ tag }}</span></div><RouterLink class="text-link" :to="`/inspirations/${post.id}`">查看并引用 →</RouterLink></div>
    </article>
  </div>
</template>
