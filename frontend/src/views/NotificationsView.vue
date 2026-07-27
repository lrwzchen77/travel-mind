<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import PagePrologue from '../components/PagePrologue.vue';
import { notificationApi } from '../api/notifications.js';

const items = ref([]);
const error = ref('');
const unread = computed(() => items.value.filter((item) => !item.read_at).length);

async function load() {
  try { items.value = await notificationApi.list(); } catch (err) { error.value = err?.message || '通知加载失败。'; }
}
async function read(item) { if (!item.read_at) { await notificationApi.read(item.id); await load(); } }
async function readAll() { await notificationApi.readAll(); await load(); }
onMounted(load);
</script>

<template>
  <PagePrologue index="08" eyebrow="消息中心" title="与你有关的更新" lead="审核结果和重要状态变化会留在这里。" />
  <div class="actions"><button type="button" class="btn-ghost" :disabled="!unread" @click="readAll">全部已读（{{ unread }}）</button></div>
  <p v-if="error" class="error-line">{{ error }}</p>
  <section class="field-stack">
    <article v-for="item in items" :key="item.id" class="glass-panel" :class="{ 'is-unread': !item.read_at }" @click="read(item)">
      <strong>{{ item.title }}</strong><small v-if="!item.read_at">未读</small><p>{{ item.content }}</p>
      <RouterLink v-if="item.target_url" class="text-link" :to="item.target_url">查看详情</RouterLink>
    </article>
    <div v-if="!items.length" class="empty-state empty-state--card">暂时没有通知。</div>
  </section>
</template>
