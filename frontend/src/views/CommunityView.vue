<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ArrowRight, Heart, MapPin } from 'lucide-vue-next';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { authSession } from '../auth/session.js';
import { communityApi } from '../api/community.js';
import { cityImageByName } from '../data/cityImages.js';
import ImageDropUpload from '../components/ImageDropUpload.vue';
import PagePrologue from '../components/PagePrologue.vue';
import { useReveal } from '../composables/useReveal.js';

const route = useRoute();
const router = useRouter();
const root = ref(null);
useReveal(root);
const posts = ref([]);
const total = ref(0);
const page = ref(1);
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
const hasMore = computed(() => posts.value.length < total.value);

function cover(post) {
  return post.cover_image || cityImageByName[String(post.city || '').replace(/市$/, '')] || '';
}

function coverAlt(post) {
  return post.cover_image ? `${post.title}封面` : `${post.city || '目的地'}旅行风景参考图`;
}

function excerpt(post) {
  const text = String(post.content || '');
  return text.length > 96 ? `${text.slice(0, 96)}…` : text;
}

async function load(pageNum = 1) {
  loading.value = true;
  error.value = '';
  try {
    const data = await communityApi.posts({ ...filters, pageNum, pageSize: 24 });
    posts.value = pageNum === 1 ? (data.records || []) : [...posts.value, ...(data.records || [])];
    page.value = pageNum;
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
    const submittedVisibility = form.visibility;
    await communityApi.createPost(form);
    Object.assign(form, { title: '', city: filters.city, topic: 'route', tags: '', cover_image: '', content: '', visibility: 'public' });
    composing.value = false;
    message.value = submittedVisibility === 'public' ? '已提交发布；审核通过后会出现在旅行社区。' : '已保存为仅自己可见，不会进入旅行社区。';
    await load();
  } catch (err) {
    error.value = err?.message || '发布失败，请稍后再试。';
  }
}

onMounted(() => load());
</script>

<template>
  <div ref="root" class="inspiration-feed-page">
    <section class="community-intro inspiration-masthead" data-reveal style="--reveal-delay: 0s">
      <PagePrologue index="03" eyebrow="旅行灵感 · 来自旅行分享" next-label="先问 AI 怎么取舍" next-to="/assistant">
        <template #title>先被一张照片打动，<br /><em>再决定下一站。</em></template>
        <template #lead>看看旅行者怎么吃、怎么玩、怎么避坑，把喜欢的体验收进行程。</template>
      </PagePrologue>
      <div class="inspiration-actions">
        <RouterLink class="btn-link btn-coral" to="/assistant">先问 AI 怎么取舍</RouterLink>
        <button type="button" class="btn-ghost" @click="startPublish">{{ composing ? '收起发布' : '发布我的分享' }}</button>
      </div>
    </section>

    <form class="community-filter inspiration-filter" data-reveal style="--reveal-delay: 0.08s" aria-label="筛选旅行灵感" @submit.prevent="load()">
      <label class="inspiration-search"><span class="visually-hidden">搜索关键词</span><input v-model="filters.keyword" placeholder="搜城市、店名、路线或避坑关键词" /></label>
      <label><span class="visually-hidden">筛选城市</span><input v-model="filters.city" placeholder="城市，例如杭州" /></label>
      <label><span class="visually-hidden">内容类型</span><select v-model="filters.topic">
        <option value="">全部类型</option>
        <option v-for="[value, label] in topics" :key="value" :value="value">{{ label }}</option>
      </select></label>
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
    <div class="inspiration-feed-head" data-reveal style="--reveal-delay: 0.16s">
      <div><span>此刻值得出发的画面</span><h2>{{ loading ? '正在翻旅行分享…' : `${total} 篇旅行者分享` }}</h2></div>
      <RouterLink class="text-link" to="/inspiration-bag">打开灵感包 <ArrowRight :size="15" :stroke-width="2.2" /></RouterLink>
    </div>

    <div v-if="!loading && !posts.length" class="empty-state empty-state--card" data-reveal style="--reveal-delay: 0.24s"><strong>还没有匹配的灵感</strong><p>换个城市或关键词试试，也可以成为第一位分享的人。</p></div>
    <div v-else class="travel-photo-feed" data-reveal style="--reveal-delay: 0.24s">
      <article v-for="post in posts" :key="post.id" class="travel-photo-tile">
        <RouterLink :to="`/inspirations/${post.id}`" class="travel-photo-link" :aria-label="`查看${post.title}`">
          <img v-if="cover(post)" :src="cover(post)" :alt="coverAlt(post)" loading="lazy" />
          <span v-else class="travel-photo-fallback" aria-hidden="true">{{ post.city || '去远方' }}</span>
          <span class="travel-photo-shade" aria-hidden="true" />
          <span class="travel-photo-topic">{{ topicLabel[post.topic] || '旅行分享' }}</span>
          <span class="travel-photo-copy">
            <span class="travel-photo-place"><MapPin :size="13" :stroke-width="2" /> {{ post.city || '目的地待补充' }} · {{ post.author || '旅行者' }}</span>
            <strong>{{ post.title }}</strong>
            <span class="travel-photo-excerpt">{{ excerpt(post) }}</span>
            <span class="travel-photo-tags">
              <span v-for="tag in String(post.tags || '').split(/[,，、\s]+/).filter(Boolean).slice(0, 3)" :key="tag">#{{ tag }}</span>
            </span>
            <span class="travel-photo-foot"><span><Heart :size="13" :stroke-width="2" /> {{ post.like_count || 0 }} · 评论 {{ post.comment_count || 0 }}</span><b>查看并引用 <ArrowRight :size="15" :stroke-width="2.2" /></b></span>
          </span>
        </RouterLink>
      </article>
    </div>
    <div v-if="hasMore" class="load-more"><button type="button" class="btn-ghost" :disabled="loading" @click="load(page + 1)">{{ loading ? '正在加载…' : `加载更多（还有 ${total - posts.length} 篇）` }}</button></div>

    <p class="inspiration-disclosure">社区内容可能来自用户经验或演示数据，价格、营业和安全信息请在出发前再次确认。</p>

    <section class="chapter-bridge" data-reveal style="--reveal-delay: 0.32s">
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章</p>
        <h2 class="chapter-bridge-title">看完了别人的分享，动手规划自己的</h2>
        <p class="chapter-bridge-lead">把社区里收藏的体验带进行程，或者直接问 AI 怎么取舍。</p>
      </div>
      <RouterLink class="chapter-bridge-cta" to="/planning">
        <span>去规划</span>
        <ArrowRight :size="18" :stroke-width="2.2" />
      </RouterLink>
    </section>
  </div>
</template>

<style scoped>
.inspiration-feed-page {
  min-width: 0;
}

.inspiration-masthead {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 32px;
  padding: 30px 4px 28px;
  border-bottom: 1px solid var(--tm-line);
}

.inspiration-kicker,
.inspiration-feed-head span,
.travel-photo-place {
  letter-spacing: .08em;
}

.inspiration-kicker {
  margin: 0 0 10px;
  color: var(--tm-accent);
  font-size: 12px;
  font-weight: 800;
}

.inspiration-masthead h1 {
  margin: 0;
  color: var(--tm-ink);
  font-family: var(--font-display);
  font-size: clamp(34px, 5.2vw, 58px);
  font-weight: 700;
  line-height: 1.12;
  letter-spacing: -.04em;
}

.inspiration-masthead h1 em {
  color: var(--tm-accent);
  font-style: normal;
}

.inspiration-masthead .lead {
  max-width: 38em;
  margin: 16px 0 0;
  color: var(--muted);
}

.inspiration-actions {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 10px;
}

.inspiration-filter {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(220px, 1.5fr) minmax(150px, .7fr) minmax(130px, .55fr) auto;
  gap: 10px;
  margin: 22px 0 36px;
  padding: 10px;
  border: 1px solid var(--tm-line);
  border-radius: 14px;
  background: var(--tm-paper);
  box-shadow: var(--tm-shadow-card);
}

.inspiration-filter input,
.inspiration-filter select {
  width: 100%;
  height: 46px;
  border: 0;
  background: var(--tm-paper-muted);
  box-shadow: none;
}

.inspiration-filter input:focus,
.inspiration-filter select:focus {
  outline: 2px solid var(--tm-accent);
  outline-offset: 1px;
  box-shadow: none;
}

.inspiration-filter button {
  min-width: 100px;
}

.community-compose {
  margin-bottom: 28px;
}

.inspiration-feed-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.inspiration-feed-head span {
  color: var(--muted);
  font-size: 11px;
  font-weight: 700;
}

.inspiration-feed-head h2 {
  margin: 4px 0 0;
  color: var(--tm-ink);
  font-size: clamp(22px, 3vw, 30px);
}

.travel-photo-feed {
  column-count: 3;
  column-gap: 16px;
}

.travel-photo-tile {
  display: inline-block;
  width: 100%;
  min-height: 430px;
  margin: 0 0 16px;
  overflow: hidden;
  break-inside: avoid;
  border-radius: 18px;
  background: var(--tm-paper);
  box-shadow: var(--tm-shadow-card);
  vertical-align: top;
}

/* Stable pseudo-random rhythm: DOM position controls size, so refreshes never jump. */
.travel-photo-tile:nth-child(7n + 2),
.travel-photo-tile:nth-child(7n + 6) { min-height: 330px; }
.travel-photo-tile:nth-child(7n + 3) { min-height: 500px; }
.travel-photo-tile:nth-child(7n + 5) { min-height: 370px; }

.travel-photo-link {
  position: relative;
  display: flex;
  min-height: inherit;
  overflow: hidden;
  color: #fff;
  isolation: isolate;
}

.travel-photo-link > img,
.travel-photo-fallback,
.travel-photo-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.travel-photo-link > img {
  object-fit: cover;
  transition: transform .55s cubic-bezier(.2, .8, .2, 1), filter .35s ease;
}

.travel-photo-fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, var(--tm-accent-deep), var(--tm-canvas-2));
  color: rgba(255, 255, 255, .44);
  font-family: var(--font-display);
  font-size: 34px;
}

.travel-photo-shade {
  z-index: 1;
  background: linear-gradient(180deg, rgba(12, 10, 8, .06) 20%, rgba(12, 10, 8, .3) 52%, rgba(12, 10, 8, .92) 100%);
}

.travel-photo-topic {
  position: absolute;
  top: 16px;
  right: 0;
  z-index: 2;
  padding: 7px 13px 7px 15px;
  border-radius: 14px 0 0 14px;
  background: var(--tm-paper-muted);
  color: var(--tm-ink);
  font-size: 11px;
  font-weight: 800;
  backdrop-filter: blur(8px);
}

.travel-photo-copy {
  position: relative;
  z-index: 2;
  align-self: flex-end;
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 9px;
  padding: 72px 20px 20px;
}

.travel-photo-place {
  color: rgba(255, 255, 255, .76);
  font-size: 11px;
  font-weight: 700;
}

.travel-photo-copy > strong {
  font-family: var(--font-display);
  font-size: clamp(23px, 2.4vw, 29px);
  line-height: 1.2;
  text-wrap: balance;
  text-shadow: 0 2px 14px rgba(0, 0, 0, .28);
}

.travel-photo-excerpt {
  display: -webkit-box;
  overflow: hidden;
  color: rgba(255, 255, 255, .84);
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.travel-photo-tags,
.travel-photo-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
}

.travel-photo-tags {
  color: #ffd9c9;
  font-size: 11px;
  font-weight: 700;
}

.travel-photo-foot {
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, .2);
  color: rgba(255, 255, 255, .7);
  font-size: 11px;
}

.travel-photo-foot b {
  color: #fff;
  font-size: 12px;
}

.travel-photo-link:hover > img {
  transform: scale(1.045);
  filter: saturate(1.08);
}

.travel-photo-link:focus-visible {
  outline: 3px solid var(--tm-accent);
  outline-offset: -3px;
}

.inspiration-disclosure {
  margin: 24px auto 0;
  max-width: 52em;
  color: var(--muted);
  font-size: 12px;
  text-align: center;
}

@media (max-width: 900px) {
  .inspiration-masthead { align-items: flex-start; flex-direction: column; }
  .inspiration-filter { grid-template-columns: 1fr 1fr; }
  .inspiration-search { grid-column: 1 / -1; }
  .travel-photo-feed { column-count: 2; }
}

@media (max-width: 620px) {
  .inspiration-masthead { padding-top: 14px; }
  .inspiration-masthead h1 { font-size: 38px; }
  .inspiration-actions { width: 100%; }
  .inspiration-actions > * { flex: 1 1 0; justify-content: center; padding-inline: 12px; white-space: nowrap; }
  .inspiration-filter { grid-template-columns: 1fr 1fr; margin-bottom: 28px; }
  .inspiration-search,
  .inspiration-filter button { grid-column: 1 / -1; }
  .inspiration-filter button { min-height: 46px; }
  .travel-photo-feed { column-count: 1; }
  .travel-photo-tile,
  .travel-photo-tile:nth-child(n) { min-height: 410px; }
  .travel-photo-tile:nth-child(4n + 2) { min-height: 330px; }
  .travel-photo-copy { padding-inline: 17px; }
}

@media (prefers-reduced-motion: reduce) {
  .travel-photo-link > img { transition: none; }
}
</style>
