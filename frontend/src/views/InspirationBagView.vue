<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { communityApi } from '../api/community.js';

const router = useRouter();
const items = ref([]);
const selected = ref([]);
const loading = ref(false);
const error = ref('');
const intentLabels = { must: '必须安排', priority: '优先参考', reference: '仅作参考' };
const selectedCount = computed(() => selected.value.length);
const overLimit = computed(() => Math.max(0, selectedCount.value - 5));

async function load() {
  loading.value = true;
  try { const data = await communityApi.bag(); items.value = data.records || []; selected.value = items.value.map((item) => item.post_id); } catch (err) { error.value = err?.message || '灵感包暂时打不开。'; } finally { loading.value = false; }
}

async function updateIntent(item) {
  try { await communityApi.addToBag(item.post_id, item.intent); } catch (err) { error.value = err?.message || '优先级保存失败。'; }
}

async function remove(item) {
  try { await communityApi.removeFromBag(item.post_id); await load(); } catch (err) { error.value = err?.message || '移除失败。'; }
}

function plan() { router.push({ path: '/map', query: { inspirationIds: selected.value.join(',') } }); }
function ask() { router.push({ path: '/assistant', query: { inspirationIds: selected.value.join(',') } }); }
onMounted(load);
</script>

<template>
  <section class="page-intro"><p class="eyebrow">我的灵感包</p><h1>这一趟，想把哪些体验带上？</h1></section>
  <p v-if="error" class="error-line">{{ error }}</p>
  <div class="section-head"><div><h2>{{ loading ? '正在整理…' : `${items.length} 篇社区分享` }}</h2></div><div class="actions"><button type="button" class="btn-ghost" :disabled="!selectedCount" @click="ask">先问 AI</button><button type="button" class="btn-coral" :disabled="!selectedCount || selectedCount > 5" @click="plan">带去规划（{{ selectedCount }}）</button></div></div>
  <p v-if="overLimit" class="error-line">规划最多引用 5 篇，请再取消 {{ overLimit }} 篇，或<button type="button" class="text-action text-action--primary" @click="ask">让 AI 先帮我取舍</button>。</p>
  <p v-else-if="items.length" class="panel-hint">最多选择 5 篇，当前已选 {{ selectedCount }} 篇。</p>
  <div v-if="!loading && !items.length" class="empty-state empty-state--card"><strong>灵感包还是空的</strong><p>在旅行社区里挑几篇吃、住、玩或避坑分享，再回来组合。</p><RouterLink class="btn-link btn-coral" to="/inspirations">去旅行社区</RouterLink></div>
  <div v-else class="bag-list"><article v-for="item in items" :key="item.post_id" class="bag-item glass-panel"><input v-model="selected" :value="item.post_id" type="checkbox" :aria-label="`选择${item.title}`" /><div><p class="eyebrow">{{ item.city || '目的地' }} · {{ item.topic || '旅行分享' }}</p><RouterLink :to="`/inspirations/${item.post_id}`"><h2>{{ item.title }}</h2></RouterLink><p>{{ String(item.content || '').slice(0, 110) }}{{ String(item.content || '').length > 110 ? '…' : '' }}</p></div><div class="bag-actions"><select v-model="item.intent" @change="updateIntent(item)"><option value="must">必须安排</option><option value="priority">优先参考</option><option value="reference">仅作参考</option></select><span>{{ intentLabels[item.intent] }}</span><button type="button" class="text-action" @click="remove(item)">移除</button></div></article></div>
</template>
