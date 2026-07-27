<script setup>
import { onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { http } from '../../api/http.js';
import { useReveal } from '../../composables/useReveal.js';
import PagePrologue from '../../components/PagePrologue.vue';
import {
  AlertCircle,
  ArrowRight,
  BrainCircuit,
  CheckCircle2,
  MapPinned,
  Save,
  Sparkles,
} from 'lucide-vue-next';

const root = ref(null);
useReveal(root);

const form = reactive({
  vite_amap_web_key: '',
  xhs_cookie: '',
  xhs_mode: 'tool',
  openai_api_key: '',
  openai_base_url: '',
  openai_model: '',
});
const configured = ref({});
const message = ref('');
const error = ref('');

async function load() {
  try {
    const response = await http.get('/admin/settings');
    configured.value = response.data?.data || response.data || {};
    Object.keys(form).forEach((key) => {
      if (configured.value[key] !== 'configured') form[key] = configured.value[key] || form[key];
    });
  } catch (err) {
    error.value = err?.message || '配置读取失败';
  }
}

async function save() {
  error.value = '';
  message.value = '';
  const updates = Object.fromEntries(Object.entries(form).filter(([, value]) => value !== ''));
  try {
    await http.put('/admin/settings', updates);
    message.value = '配置已更新，本次 Java 进程立即生效。';
    await load();
  } catch (err) {
    error.value = err?.message || '配置保存失败';
  }
}

onMounted(load);
</script>

<template>
  <div ref="root" class="admin-settings-page">
    <section class="admin-hero admin-hero--slim" data-reveal>
      <PagePrologue
        index="A1 · 配置"
        eyebrow="System Settings"
        title="外部服务与<em>模型连接</em>"
        lead="管理地图、内容采集与大模型上游凭证；保存后立即在 Java 服务端生效。"
      />
    </section>

    <div class="admin-section-head" data-reveal>
      <div>
        <p class="eyebrow">配置项</p>
        <h2>运行依赖</h2>
      </div>
    </div>

    <div v-if="message" class="admin-status-line is-success" data-reveal>
      <CheckCircle2 :size="16" aria-hidden="true" />
      <span>{{ message }}</span>
    </div>
    <div v-if="error" class="admin-status-line is-error" data-reveal>
      <AlertCircle :size="16" aria-hidden="true" />
      <span>{{ error }}</span>
    </div>

    <form class="admin-settings-form" data-reveal @submit.prevent="save">
      <section data-reveal>
        <h2 data-en="MAP SERVICE">
          <span class="admin-settings-section-title">
            <MapPinned :size="17" aria-hidden="true" />
            地图服务
          </span>
        </h2>
        <label>
          <span>高德 Web Service Key</span>
          <input
            v-model="form.vite_amap_web_key"
            type="password"
            :placeholder="configured.vite_amap_web_key === 'configured' ? '已配置，留空则保持不变' : '请输入 Key'"
          />
        </label>
      </section>

      <section data-reveal>
        <h2 data-en="XIAOHONGSHU CONTENT">
          <span class="admin-settings-section-title">
            <Sparkles :size="17" aria-hidden="true" />
            小红书内容
          </span>
        </h2>
        <label>
          <span>Cookie</span>
          <textarea
            v-model="form.xhs_cookie"
            rows="4"
            :placeholder="configured.xhs_cookie === 'configured' ? '已配置，留空则保持不变' : '请输入 Cookie'"
          />
        </label>
        <label>
          <span>采集模式</span>
          <select v-model="form.xhs_mode">
            <option value="service">服务采集</option>
            <option value="tool">智能体工具</option>
            <option value="both">双链路合并</option>
          </select>
        </label>
      </section>

      <section data-reveal>
        <h2 data-en="LLM CONNECTION">
          <span class="admin-settings-section-title">
            <BrainCircuit :size="17" aria-hidden="true" />
            大模型
          </span>
        </h2>
        <label>
          <span>API Key</span>
          <input
            v-model="form.openai_api_key"
            type="password"
            :placeholder="configured.openai_api_key === 'configured' ? '已配置，留空则保持不变' : '请输入 API Key'"
          />
        </label>
        <label>
          <span>服务地址</span>
          <input v-model="form.openai_base_url" placeholder="https://api.deepseek.com" />
        </label>
        <label>
          <span>模型名称</span>
          <input v-model="form.openai_model" placeholder="deepseek-v4-flash" />
        </label>
      </section>

      <div class="admin-settings-actions" data-reveal>
        <button type="submit" class="admin-primary-btn">
          <Save :size="16" aria-hidden="true" />
          保存配置
        </button>
      </div>
    </form>

    <section class="chapter-bridge" data-reveal>
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章</p>
        <h2 class="chapter-bridge-title">返回运营总览</h2>
        <p class="chapter-bridge-lead">完成配置后，回到指挥台查看模型、资源与业务指标的实时状态。</p>
      </div>
      <RouterLink class="chapter-bridge-cta" to="/admin">
        <span>去运营总览</span>
        <ArrowRight :size="18" :stroke-width="2.2" aria-hidden="true" />
      </RouterLink>
    </section>
  </div>
</template>
