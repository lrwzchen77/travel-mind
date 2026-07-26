<script setup>
import { onMounted, reactive, ref } from 'vue';
import { http } from '../../api/http.js';
import { useReveal } from '../../composables/useReveal.js';

const root = ref(null);
useReveal(root);

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
  <div ref="root" class="admin-settings-page">
    <section class="admin-page-head"><div><p>系统配置</p><h1>外部服务与模型连接</h1></div></section>
    <p v-if="message" class="success-line">{{ message }}</p><p v-if="error" class="error-line">{{ error }}</p>
    <form class="admin-settings-form" data-reveal @submit.prevent="save">
      <section data-reveal style="--reveal-delay: 0.1s"><h2 data-en="MAP SERVICE">地图服务</h2><label><span>高德 Web Service Key</span><input v-model="form.vite_amap_web_key" type="password" :placeholder="configured.vite_amap_web_key === 'configured' ? '已配置，留空则保持不变' : '请输入 Key'" /></label></section>
      <section data-reveal style="--reveal-delay: 0.1s"><h2 data-en="XIAOSHU CONTENT">小红书内容</h2><label><span>Cookie</span><textarea v-model="form.xhs_cookie" rows="4" :placeholder="configured.xhs_cookie === 'configured' ? '已配置，留空则保持不变' : '请输入 Cookie'" /></label><label><span>采集模式</span><select v-model="form.xhs_mode"><option value="service">服务采集</option><option value="tool">智能体工具</option><option value="both">双链路合并</option></select></label></section>
      <section data-reveal style="--reveal-delay: 0.1s"><h2 data-en="LLM CONNECTION">大模型</h2><label><span>API Key</span><input v-model="form.openai_api_key" type="password" :placeholder="configured.openai_api_key === 'configured' ? '已配置，留空则保持不变' : '请输入 API Key'" /></label><label><span>服务地址</span><input v-model="form.openai_base_url" placeholder="https://api.deepseek.com" /></label><label><span>模型名称</span><input v-model="form.openai_model" placeholder="deepseek-v4-flash" /></label></section>
      <button type="submit" class="admin-primary-btn">保存配置</button>
    </form>
  </div>
</template>

<style scoped>
.admin-settings-page { max-width: 920px; }
.admin-page-head p {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--tm-accent) !important;
  font-family: var(--font-mono);
  font-size: 11px !important;
  font-weight: 700 !important;
  letter-spacing: 0.24em !important;
  text-transform: uppercase;
}
.admin-page-head p::before {
  content: "";
  width: 24px;
  height: 1px;
  background: var(--tm-accent);
}
.admin-page-head h1 {
  font-family: var(--font-display) !important;
  font-size: clamp(28px, 4vw, 38px) !important;
  letter-spacing: -0.035em !important;
  line-height: 1.1 !important;
}

/* ── Section headers: editorial eyebrow + accent rail ── */
.admin-settings-form section {
  position: relative;
  padding-left: 22px;
}
.admin-settings-form section::before {
  content: "";
  position: absolute;
  left: 0;
  top: 32px;
  bottom: 12px;
  width: 1px;
  background: linear-gradient(180deg, var(--tm-accent) 0%, transparent 100%);
  opacity: 0.5;
}
.admin-settings-form h2 {
  position: relative;
  margin: 0 0 22px !important;
  padding-left: 16px;
  font-family: var(--font-display) !important;
  font-size: 18px !important;
  font-weight: 700 !important;
  letter-spacing: -0.015em !important;
  color: var(--tm-ink) !important;
  line-height: 1.2;
}
.admin-settings-form h2::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 10px;
  height: 2px;
  background: var(--tm-accent);
  transform: translateY(-50%);
}
.admin-settings-form h2::after {
  content: attr(data-en);
  display: block;
  margin-top: 5px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: var(--tm-muted);
}

/* ── Inputs: editorial focus state ── */
.admin-settings-form input,
.admin-settings-form textarea,
.admin-settings-form select {
  transition: border-color 0.25s ease, box-shadow 0.25s ease, background 0.25s ease;
}
.admin-settings-form input:focus,
.admin-settings-form textarea:focus,
.admin-settings-form select:focus {
  border-color: var(--tm-accent) !important;
  box-shadow: 0 0 0 3px var(--tm-accent-soft) !important;
  outline: none;
  background: var(--tm-paper-raised) !important;
}
.admin-settings-form label:hover span {
  color: var(--tm-ink);
}

/* ── Save button: cinematic CTA ── */
.admin-primary-btn {
  position: relative;
  overflow: hidden;
  margin-top: 12px;
  padding: 14px 28px !important;
  border-radius: var(--tm-radius-pill) !important;
  background: linear-gradient(135deg, var(--tm-accent) 0%, var(--tm-accent-deep) 100%) !important;
  color: #160d05 !important;
  font-family: var(--font-display) !important;
  font-size: 14px !important;
  font-weight: 700 !important;
  letter-spacing: 0.02em;
  box-shadow: 0 12px 30px -12px var(--tm-accent-glow), inset 0 1px 0 rgba(255, 255, 255, 0.25);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s ease;
}
.admin-primary-btn::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(105deg, transparent 40%, rgba(255, 255, 255, 0.32) 50%, transparent 60%);
  transform: translateX(-120%);
  transition: transform 0.8s cubic-bezier(0.16, 1, 0.3, 1);
  pointer-events: none;
}
.admin-primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 40px -12px var(--tm-accent-glow), inset 0 1px 0 rgba(255, 255, 255, 0.3);
}
.admin-primary-btn:hover::after { transform: translateX(120%); }
.admin-primary-btn:active { transform: translateY(0); }
</style>
