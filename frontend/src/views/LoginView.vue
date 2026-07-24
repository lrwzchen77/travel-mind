<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { authApi } from '../api/auth.js';

const route = useRoute();
const router = useRouter();
const portal = computed(() => route.meta.portal || 'user');
const isAdmin = computed(() => portal.value === 'admin');
const form = reactive({ username: '', password: '' });
const loading = ref(false);
const error = ref('');
const cityIndex = ref(0);
const demoCredentialText = import.meta.env.DEV
  ? '本地演示默认账号为 demo_user、密码为 travel123；如果已修改环境配置，以实际配置为准。'
  : '';

const cities = ['杭州', '成都', '厦门', '西安', '大理', '青岛'];

let cityTimer;

async function submit() {
  loading.value = true;
  error.value = '';
  try {
    await authApi.login(portal.value, form);
    const fallback = isAdmin.value ? '/admin' : '/';
    await router.replace(isAdmin.value ? fallback : String(route.query.redirect || fallback));
  } catch (err) {
    error.value = err?.message || '登录失败，请检查账号和密码';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  if (!isAdmin.value) {
    cityTimer = window.setInterval(() => {
      cityIndex.value = (cityIndex.value + 1) % cities.length;
    }, 2600);
  }
});

onUnmounted(() => {
  if (cityTimer) window.clearInterval(cityTimer);
});
</script>

<template>
  <!-- 运营登录：居中简版 -->
  <main v-if="isAdmin" class="login-page login-page--admin">
    <RouterLink to="/admin/login" class="login-brand login-brand--admin">
      <strong>Travel Mind</strong>
      <span>运营管理中心</span>
    </RouterLink>
    <section class="login-panel login-panel--admin">
      <p class="eyebrow">管理员登录</p>
      <h1>进入运营工作台</h1>
      <form class="field-stack" @submit.prevent="submit">
        <label>
          <span class="field-label">账号</span>
          <input v-model.trim="form.username" autocomplete="username" placeholder="管理员账号" required />
        </label>
        <label>
          <span class="field-label">密码</span>
          <input v-model="form.password" type="password" autocomplete="current-password" placeholder="输入密码" required />
        </label>
        <p v-if="error" class="error-line">{{ error }}</p>
        <button type="submit" class="btn-coral" :disabled="loading">
          {{ loading ? '正在登录…' : '进入工作台' }}
        </button>
      </form>
      <div class="login-switch">
        <RouterLink to="/login">返回旅行账号登录</RouterLink>
      </div>
    </section>
  </main>

  <!-- 用户登录：视口内左右分屏，不再上下漂 -->
  <main v-else class="login-split">
    <aside class="login-showcase">
      <div class="login-showcase-mesh" aria-hidden="true" />
      <div class="login-showcase-glow" aria-hidden="true" />

      <RouterLink to="/" class="login-showcase-brand">
        <span class="brand-mark" aria-hidden="true"><span class="brand-plane">✈</span></span>
        <span>
          <strong>Travel Mind</strong>
          <small>想去，就出发</small>
        </span>
      </RouterLink>

      <div class="login-showcase-main">
        <div class="login-showcase-copy">
          <p class="login-showcase-kicker">
            <span class="pulse-dot" aria-hidden="true" />
            这个周末去哪玩？
          </p>
          <h1 class="login-headline">
            <span class="login-headline-fixed">下一站，</span>
            <span class="login-city" :key="cities[cityIndex]">{{ cities[cityIndex] }}</span>
          </h1>
        </div>

        <div class="login-ticket" aria-hidden="true">
          <div class="login-pass">
            <div class="login-pass-top">
              <span>BOARDING PASS</span>
              <span>TM · 2026</span>
            </div>
            <div class="login-pass-mid">
              <div>
                <small>FROM</small>
                <strong>日常</strong>
              </div>
              <div class="login-pass-plane">✈</div>
              <div>
                <small>TO</small>
                <strong>{{ cities[cityIndex] }}</strong>
              </div>
            </div>
            <div class="login-pass-bot">
              <span>智能排程</span>
              <span>可保存 · 可对话</span>
            </div>
          </div>
        </div>
      </div>

    </aside>

    <section class="login-form-side">
      <div class="login-form-card">
        <p class="eyebrow">欢迎回来</p>
        <h1>继续你的下一程</h1>

        <form class="field-stack" @submit.prevent="submit">
          <label>
            <span class="field-label">用户名</span>
            <input
              v-model.trim="form.username"
              autocomplete="username"
              placeholder="你的旅行账号"
              required
            />
          </label>
          <label>
            <span class="field-label">密码</span>
            <input
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              placeholder="输入密码"
              required
            />
          </label>
          <p v-if="error" class="error-line">{{ error }}</p>
          <button type="submit" class="btn-coral login-submit" :disabled="loading">
            {{ loading ? '正在登录…' : '登录，出发' }}
            <span aria-hidden="true">→</span>
          </button>
        </form>

        <details class="login-help">
          <summary>第一次来？查看登录说明</summary>
          <p>当前版本还未开放注册和找回密码，请使用已有账号或联系服务提供方。<template v-if="demoCredentialText">{{ demoCredentialText }}</template></p>
        </details>

        <details class="login-help">
          <summary>服务边界、用户协议与隐私说明</summary>
          <p>本产品提供旅行信息整理和 AI 规划建议，不直接完成酒店、门票或交通预订。价格、库存和营业信息以实际服务方为准。登录后会保存账号信息、行程、偏好及你主动上传的内容，用于提供规划和回忆功能；未经你确认，私密旅行记录不会发布到社区。演示环境请勿上传身份证件、支付凭证等敏感信息。</p>
        </details>

        <div class="login-switch">
          <RouterLink to="/">先逛逛目的地</RouterLink>
        </div>
      </div>
    </section>
  </main>
</template>
