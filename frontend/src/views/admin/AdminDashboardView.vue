<script setup>
import { onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { resourceApi } from '../../api/resources.js';

const metrics = ref([]);
const loading = ref(true);
onMounted(async () => {
  const definitions = [['users', '用户'], ['trip-plans', '行程'], ['cities', '城市'], ['map-pois', '地图地点']];
  metrics.value = await Promise.all(definitions.map(async ([key, label]) => {
    try { return { key, label, value: (await resourceApi.list(key, { pageSize: 1 })).total || 0 }; }
    catch { return { key, label, value: '—' }; }
  }));
  loading.value = false;
});
</script>

<template>
  <section class="admin-page-head"><div><p>运营总览</p><h1>平台今天运行得怎么样</h1></div><span>{{ loading ? '数据同步中' : '数据已更新' }}</span></section>
  <div class="admin-metrics"><article v-for="item in metrics" :key="item.key"><span>{{ item.label }}</span><strong>{{ item.value }}</strong><RouterLink :to="`/admin/resources/${item.key}`">查看数据</RouterLink></article></div>
  <section class="admin-section"><div class="admin-section-head"><div><h2>常用工作</h2></div></div><div class="admin-quick-grid"><RouterLink to="/admin/resources/cities"><strong>目的地内容</strong></RouterLink><RouterLink to="/admin/resources/users"><strong>用户治理</strong></RouterLink><RouterLink to="/admin/settings"><strong>运行配置</strong></RouterLink></div></section>
</template>
