<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { Sparkles, ArrowRight } from 'lucide-vue-next';
import { communityApi } from '../api/community.js';

const router = useRouter();
const open = ref(false);
const items = ref([]);
const loading = ref(false);
const error = ref('');
const count = computed(() => items.value.length);
const preview = computed(() => items.value.slice(0, 3));

async function load() {
  loading.value = true;
  try {
    items.value = (await communityApi.bag()).records || [];
    error.value = '';
  } catch {
    items.value = [];
    error.value = '灵感包暂时不可用';
  } finally {
    loading.value = false;
  }
}

function close() { open.value = false; }
function onDocClick(event) {
  if (!event.target.closest?.('.inspiration-bag-float')) close();
}
function plan() {
  close();
  router.push({ path: '/map', query: { inspirationIds: items.value.map((item) => item.post_id).slice(0, 5).join(',') } });
}

onMounted(() => {
  load();
  document.addEventListener('click', onDocClick);
  window.addEventListener('inspiration-bag-changed', load);
});
onUnmounted(() => {
  document.removeEventListener('click', onDocClick);
  window.removeEventListener('inspiration-bag-changed', load);
});
</script>

<template>
  <div class="inspiration-bag-float" :class="{ 'is-open': open }">
    <button
      type="button"
      class="inspiration-bag-trigger"
      :aria-expanded="open"
      aria-label="打开灵感包"
      @click.stop="open = !open"
    >
      <span class="inspiration-bag-icon" aria-hidden="true"><Sparkles :size="20" :stroke-width="2" /></span>
      <span><strong>灵感包</strong><small>{{ count ? `${count} 篇待规划` : '去挑攻略' }}</small></span>
      <b v-if="count">{{ count }}</b>
    </button>

    <aside v-show="open" class="inspiration-bag-popover" aria-label="灵感包预览">
      <div class="inspiration-bag-popover-head">
        <div><p class="eyebrow">本次旅行素材</p><h2>{{ count ? `${count} 篇已加入` : '灵感包还是空的' }}</h2></div>
        <RouterLink to="/inspiration-bag" @click="close">管理 <ArrowRight :size="13" :stroke-width="2.2" /></RouterLink>
      </div>
      <p v-if="error" class="error-line">{{ error }}</p>
      <p v-else-if="loading" class="panel-hint">正在整理灵感…</p>
      <template v-else-if="count">
        <RouterLink
          v-for="item in preview"
          :key="item.post_id"
          class="inspiration-bag-preview"
          :to="`/inspirations/${item.post_id}`"
          @click="close"
        >
          <span :class="`is-${item.intent}`">{{ item.intent === 'must' ? '必去' : item.intent === 'priority' ? '优先' : '参考' }}</span>
          <strong>{{ item.title }}</strong>
        </RouterLink>
        <p v-if="count > preview.length" class="panel-hint">另有 {{ count - preview.length }} 篇灵感待组合</p>
        <div class="inspiration-bag-popover-actions">
          <RouterLink class="btn-link btn-ghost" to="/assistant" @click="close">先问 AI</RouterLink>
          <button type="button" class="btn-coral" @click="plan">去生成行程</button>
        </div>
      </template>
      <div v-else class="inspiration-bag-empty">
        <RouterLink class="btn-link btn-coral" to="/inspirations" @click="close">去旅行社区</RouterLink>
      </div>
    </aside>
  </div>
</template>
