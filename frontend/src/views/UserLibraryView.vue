<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { resourceApi } from '../api/resources.js';
import { cityImageByName } from '../data/cityImages.js';

const route = useRoute();
const items = ref([]);
const loading = ref(false);
const error = ref('');
const message = ref('');
const composing = ref(false);
const form = reactive({ title: '', content: '', visibility: 'private' });

const resourceKey = computed(() => route.meta.resourceKey);
const pageTitle = computed(() => route.meta.title);

const pageCopy = computed(() => {
  const map = {
    favorites: {
      eyebrow: '我的收藏',
      emptyTitle: '收藏夹还是空的',
      emptyHint: '在发现页点「收藏」，下一程灵感就不会丢。',
      emptyCta: { to: '/cities', label: '去发现城市' },
      count: (n) => (n ? `收了 ${n} 份灵感` : '还没有收藏'),
    },
    'travel-notes': {
      eyebrow: '旅行笔记',
      emptyTitle: '还没有写下第一篇',
      emptyHint: '旅途中的一句吐槽、一顿好饭，都值得记下来。',
      emptyCta: null,
      count: (n) => (n ? `${n} 篇笔记` : '笔记本空着'),
    },
    'ai-records': {
      eyebrow: '分析足迹',
      emptyTitle: '还没有分析记录',
      emptyHint: '去 AI 灵感里贴一段游记，提炼下一站线索。',
      emptyCta: { to: '/ai-lab', label: '试试 AI 灵感' },
      count: (n) => (n ? `${n} 次灵感提炼` : '足迹是空的'),
    },
  };
  return map[resourceKey.value] || {
    eyebrow: '我的旅行',
    emptyTitle: '这里还是空的',
    emptyHint: '旅行中的灵感会慢慢积累在这里。',
    emptyCta: { to: '/planning', label: '开始规划' },
    count: (n) => `${n} 项`,
  };
});

const canCreate = computed(() => resourceKey.value === 'travel-notes');

function itemKind(item) {
  if (resourceKey.value === 'favorites') {
    const t = item.target_type;
    const labels = {
      city: '城市',
      attraction: '景点',
      hotel: '住宿',
      restaurant: '美食',
    };
    return labels[t] || '收藏';
  }
  if (resourceKey.value === 'ai-records') {
    return item.analysis_type || '灵感分析';
  }
  if (item.visibility === 'public') return '已分享';
  if (item.visibility === 'private') return '仅自己';
  return '笔记';
}

function itemTitle(item) {
  return item.title || item.note || item.result_summary || '未命名内容';
}

function itemBody(item) {
  return item.content || item.request_summary || item.note || '保存在你的旅行空间';
}

function favoriteCityLink(item) {
  return `/city/${encodeURIComponent(item.note || '')}`;
}

async function load() {
  loading.value = true;
  error.value = '';
  message.value = '';
  try {
    items.value = (await resourceApi.userList(resourceKey.value, { pageSize: 30 })).records || [];
  } catch (err) {
    error.value = err?.message || '加载失败，请稍后再试';
  } finally {
    loading.value = false;
  }
}

async function create() {
  try {
    await resourceApi.userCreate(resourceKey.value, {
      title: form.title,
      content: form.content,
      visibility: form.visibility,
      status: 1,
    });
    Object.assign(form, { title: '', content: '', visibility: 'private' });
    composing.value = false;
    message.value = '笔记已保存';
    await load();
  } catch (err) {
    error.value = err?.message || '保存失败';
  }
}

async function remove(id) {
  if (!window.confirm(resourceKey.value === 'favorites' ? '从收藏里拿掉？' : '删掉这篇笔记？')) return;
  try {
    await resourceApi.userRemove(resourceKey.value, id);
    message.value = resourceKey.value === 'favorites' ? '已从收藏移除' : '笔记已删除';
    await load();
  } catch (err) {
    error.value = err?.message || '操作失败';
  }
}

watch(resourceKey, () => {
  composing.value = false;
  load();
});
onMounted(load);
</script>

<template>
  <section class="page-intro">
    <p class="eyebrow">{{ pageCopy.eyebrow }}</p>
    <h1>{{ pageTitle }}</h1>
  </section>

  <div class="section-head">
    <div>
      <h2>{{ loading ? '正在打开…' : pageCopy.count(items.length) }}</h2>
    </div>
    <button
      v-if="canCreate"
      class="btn-coral"
      type="button"
      @click="composing = !composing"
    >
      {{ composing ? '收起' : '写一篇' }}
    </button>
  </div>

  <p v-if="message" class="success-line">{{ message }}</p>
  <p v-if="error" class="error-line">{{ error }}</p>

  <form
    v-if="composing"
    class="glass-panel field-stack library-compose"
    @submit.prevent="create"
  >
    <h2 style="margin: 0; font-family: var(--font-display); font-size: 20px;">新的一篇</h2>
    <div>
      <label class="field-label" for="note-title">标题</label>
      <input id="note-title" v-model="form.title" placeholder="例如：西湖边的那碗面" required />
    </div>
    <div>
      <label class="field-label" for="note-body">正文</label>
      <textarea
        id="note-body"
        v-model="form.content"
        rows="5"
        placeholder="记下路上的风景、排队的人潮、或突然想再说一次的话…"
        required
      />
    </div>
    <div>
      <label class="field-label" for="note-vis">谁能看见</label>
      <select id="note-vis" v-model="form.visibility">
        <option value="private">仅自己可见</option>
        <option value="public">公开分享</option>
      </select>
    </div>
    <div class="actions">
      <button type="submit" class="btn-coral">保存笔记</button>
      <button type="button" class="btn-ghost" @click="composing = false">取消</button>
    </div>
  </form>

  <div v-if="!loading && !items.length" class="empty-state empty-state--card">
    <strong>{{ pageCopy.emptyTitle }}</strong>
    <p>{{ pageCopy.emptyHint }}</p>
    <div v-if="pageCopy.emptyCta" class="actions" style="justify-content: center; margin-top: 16px;">
      <RouterLink class="btn-link btn-coral" :to="pageCopy.emptyCta.to">
        {{ pageCopy.emptyCta.label }}
      </RouterLink>
    </div>
    <div v-else-if="canCreate" class="actions" style="justify-content: center; margin-top: 16px;">
      <button type="button" class="btn-coral" @click="composing = true">写第一篇</button>
    </div>
  </div>

  <div v-else class="library-grid">
    <article v-for="item in items" :key="item.id" class="library-card">
      <div class="library-card-top">
        <span class="library-kind">{{ itemKind(item) }}</span>
        <button
          v-if="resourceKey !== 'ai-records'"
          type="button"
          class="library-remove"
          :aria-label="resourceKey === 'favorites' ? '取消收藏' : '删除笔记'"
          @click="remove(item.id)"
        >
          {{ resourceKey === 'favorites' ? '取消收藏' : '删除' }}
        </button>
      </div>
      <RouterLink
        v-if="resourceKey === 'favorites' && item.target_type === 'city'"
        class="library-city-favorite"
        :to="favoriteCityLink(item)"
      >
        <img
          v-if="cityImageByName[item.note]"
          :src="cityImageByName[item.note]"
          :alt="`${item.note}城市风景`"
        />
        <div>
          <h2>{{ itemTitle(item) }}</h2>
          <p>继续查看这座城 →</p>
        </div>
      </RouterLink>
      <template v-else>
        <h2>{{ itemTitle(item) }}</h2>
        <p>{{ itemBody(item) }}</p>
      </template>
    </article>
  </div>
</template>
