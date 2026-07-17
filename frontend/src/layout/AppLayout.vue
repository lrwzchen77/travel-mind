<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { accountNav, primaryNav } from './menu.js';
import PageTransition from '../components/PageTransition.vue';
import InspirationBagFloat from '../components/InspirationBagFloat.vue';
import { authApi } from '../api/auth.js';
import { authSession } from '../auth/session.js';

const route = useRoute();
const router = useRouter();
const currentUser = ref(authSession.user());
const userInitial = computed(() => String(currentUser.value?.name || '旅').trim().charAt(0));
const menuOpen = ref(false);
const accountOpen = ref(false);
const scrolled = ref(false);
const showInspirationBag = computed(() => currentUser.value && !/^\/memories(?:\/|$)/.test(route.path));

function closeMenus() {
  menuOpen.value = false;
  accountOpen.value = false;
}

function onDocClick(e) {
  if (!e.target.closest?.('.account-menu') && !e.target.closest?.('.nav-toggle')) {
    accountOpen.value = false;
  }
}

function onScroll() {
  scrolled.value = window.scrollY > 12;
}

function isNavActive(path) {
  if (route.path === path || (path !== '/' && route.path.startsWith(path))) return true;
  return (path === '/cities' && route.path.startsWith('/city/'))
    || (path === '/trip-history' && route.path.startsWith('/trip/'));
}

async function logout() {
  await authApi.logout('user');
  currentUser.value = null;
  closeMenus();
  router.replace('/');
}

watch(() => route.fullPath, () => {
  currentUser.value = authSession.user();
  closeMenus();
  window.scrollTo({ top: 0, behavior: 'smooth' });
});

onMounted(() => {
  document.addEventListener('click', onDocClick);
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();
});

onUnmounted(() => {
  document.removeEventListener('click', onDocClick);
  window.removeEventListener('scroll', onScroll);
});
</script>

<template>
  <div class="app-shell">
    <header class="site-header" :class="{ 'is-scrolled': scrolled }">
      <div class="site-header-inner">
        <RouterLink to="/" class="brand" @click="closeMenus">
          <span class="brand-mark" aria-hidden="true">
            <span class="brand-plane">✈</span>
          </span>
          <span class="brand-text">
            <strong>Travel Mind</strong>
            <small>想去，就出发</small>
          </span>
        </RouterLink>

        <button
          type="button"
          class="nav-toggle"
          :aria-expanded="menuOpen"
          aria-label="打开菜单"
          @click="menuOpen = !menuOpen"
        >
          <span /><span /><span />
        </button>

        <nav class="top-nav" :class="{ 'is-open': menuOpen }" aria-label="主导航">
          <RouterLink
            v-for="item in primaryNav"
            :key="item.path"
            :to="item.path"
            class="top-link"
            :class="{
              'is-active': isNavActive(item.path),
            }"
            @click="closeMenus"
          >
            {{ item.label }}
          </RouterLink>

          <RouterLink to="/planning" class="header-cta" @click="closeMenus">
            生成行程
          </RouterLink>
          <div v-if="currentUser" class="account-menu" :class="{ 'is-open': accountOpen }">
            <button
              type="button"
              class="account-trigger"
              aria-haspopup="true"
              :aria-expanded="accountOpen"
              @click.stop="accountOpen = !accountOpen"
            >
              <span class="account-avatar" aria-hidden="true">{{ userInitial }}</span>
              <span class="account-trigger-name">{{ currentUser.name || '旅行者' }}</span>
              <span class="account-chevron" aria-hidden="true">▾</span>
            </button>
            <div v-show="accountOpen" class="account-popover" role="menu">
              <div class="account-head">
                <strong>{{ currentUser.name || '旅行者' }}</strong>
                <span>旅行账户</span>
              </div>
              <RouterLink
                v-for="item in accountNav"
                :key="item.path"
                :to="item.path"
                class="account-link"
                role="menuitem"
                @click="closeMenus"
              >
                {{ item.label }}
              </RouterLink>
              <button type="button" class="account-logout" role="menuitem" @click="logout">
                退出登录
              </button>
            </div>
          </div>
          <RouterLink v-else to="/login" class="session-link" @click="closeMenus">登录</RouterLink>
        </nav>
      </div>
    </header>

    <main class="site-main">
      <PageTransition />
    </main>

    <InspirationBagFloat v-if="showInspirationBag" />

    <footer class="site-footer">
      <div class="site-footer-inner">
        <div>
          <strong>Travel Mind</strong>
        </div>
        <div class="footer-links">
          <RouterLink to="/planning">生成行程</RouterLink>
          <RouterLink to="/map">地图情报</RouterLink>
          <RouterLink to="/trip-history">我的行程</RouterLink>
          <RouterLink to="/cities">发现城市</RouterLink>
          <RouterLink to="/assistant">先问 AI</RouterLink>
        </div>
        <div class="footer-disclosures">
          <details id="service-terms"><summary>服务边界与用户协议</summary><p>Travel Mind 提供旅行信息整理和 AI 规划建议，不直接完成酒店、门票或交通预订。价格、库存、营业和预约要求以实际服务方为准。使用规划结果前，请根据自身健康、天气和当地规定作出判断。</p></details>
          <details id="privacy-notice"><summary>隐私与内容公开说明</summary><p>账号信息、行程、偏好和你主动上传的内容用于提供规划与回忆功能。旅行记录默认仅自己可见；只有在你确认发布并通过审核后，内容才会进入社区。演示环境请勿上传证件、支付凭证等敏感信息。当前版本尚未开放账号自助注销，如需处理数据，请联系服务提供方。</p></details>
        </div>
      </div>
    </footer>
  </div>
</template>
