<script setup>
import { onMounted, reactive, ref } from 'vue';
import { http } from '../../api/http.js';

const form = reactive({ vite_amap_web_key: '', xhs_cookie: '', xhs_mode: 'tool', openai_api_key: '', openai_base_url: '', openai_model: '' });
const configured = ref({});
const message = ref('');
const error = ref('');

async function load() {
  try { const response = await http.get('/admin/settings'); configured.value = response.data?.data || response.data || {}; Object.keys(form).forEach((key) => { if (configured.value[key] !== 'configured') form[key] = configured.value[key] || form[key]; }); }
  catch (err) { error.value = err?.message || '配置读取失败'; }
}

async function save() {
  error.value = ''; message.value = '';
  const updates = Object.fromEntries(Object.entries(form).filter(([, value]) => value !== ''));
  try { await http.put('/admin/settings', updates); message.value = '配置已更新，本次 Java 进程立即生效。'; await load(); }
  catch (err) { error.value = err?.message || '配置保存失败'; }
}
onMounted(load);
</script>

<template>
  <section class="admin-page-head"><div><p>系统配置</p><h1>外部服务与模型连接</h1></div></section>
  <p v-if="message" class="success-line">{{ message }}</p><p v-if="error" class="error-line">{{ error }}</p>
  <form class="admin-settings-form" @submit.prevent="save">
    <section><h2>地图服务</h2><p>用于真实地点、天气、酒店和路线数据。</p><label><span>高德 Web Service Key</span><input v-model="form.vite_amap_web_key" type="password" :placeholder="configured.vite_amap_web_key === 'configured' ? '已配置，留空则保持不变' : '请输入 Key'" /></label></section>
    <section><h2>小红书内容</h2><p>用于补充真实旅行笔记和图片参考。</p><label><span>Cookie</span><textarea v-model="form.xhs_cookie" rows="4" :placeholder="configured.xhs_cookie === 'configured' ? '已配置，留空则保持不变' : '请输入 Cookie'" /></label><label><span>采集模式</span><select v-model="form.xhs_mode"><option value="service">服务采集</option><option value="tool">智能体工具</option><option value="both">双链路合并</option></select></label></section>
    <section><h2>大模型</h2><p>支持 OpenAI 兼容协议，包括 DeepSeek。</p><label><span>API Key</span><input v-model="form.openai_api_key" type="password" :placeholder="configured.openai_api_key === 'configured' ? '已配置，留空则保持不变' : '请输入 API Key'" /></label><label><span>服务地址</span><input v-model="form.openai_base_url" placeholder="https://api.deepseek.com" /></label><label><span>模型名称</span><input v-model="form.openai_model" placeholder="deepseek-v4-flash" /></label></section>
    <button type="submit" class="admin-primary-btn">保存配置</button>
  </form>
</template>
