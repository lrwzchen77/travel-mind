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

const cities = ['杭州', '成都', '厦门', '西安', '大理', '青岛'];

let cityTimer;

async function submit() {
  loading.value = true;
  error.value = '';
  try {
    await authApi.login(portal.value, form);
    const fallback = isAdmin.value ? '/admin' : '/';
    await router.replace(String(route.query.redirect || fallback));
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

        <div class="login-switch">
          <RouterLink to="/">先逛逛目的地</RouterLink>
        </div>
      </div>
    </section>
  </main>
</template>
