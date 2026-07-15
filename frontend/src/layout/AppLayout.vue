<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { primaryNav, secondaryNav } from './menu.js';
import PageTransition from '../components/PageTransition.vue';

const route = useRoute();
const menuOpen = ref(false);
const moreOpen = ref(false);
const scrolled = ref(false);

function closeMenus() {
  menuOpen.value = false;
  moreOpen.value = false;
}

function onDocClick(e) {
  if (!e.target.closest?.('.nav-more') && !e.target.closest?.('.nav-toggle')) {
    moreOpen.value = false;
  }
}

function onScroll() {
  scrolled.value = window.scrollY > 12;
}

watch(() => route.fullPath, () => {
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
    <div class="ambient" aria-hidden="true">
      <span class="orb orb-a" />
      <span class="orb orb-b" />
      <span class="orb orb-c" />
    </div>

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
              'is-active':
                route.path === item.path
                || (item.path !== '/' && route.path.startsWith(item.path)),
            }"
            @click="closeMenus"
          >
            {{ item.label }}
          </RouterLink>

          <div class="nav-more" :class="{ 'is-open': moreOpen }">
            <button
              type="button"
              class="top-link top-link-btn"
              :class="{ 'is-active': secondaryNav.some((i) => route.path === i.path) }"
              @click.stop="moreOpen = !moreOpen"
            >
              更多
              <span class="chev" aria-hidden="true">▾</span>
            </button>
            <div v-show="moreOpen" class="more-panel">
              <RouterLink
                v-for="item in secondaryNav"
                :key="item.path"
                :to="item.path"
                class="more-link"
                @click="closeMenus"
              >
                {{ item.label }}
              </RouterLink>
            </div>
          </div>

          <RouterLink to="/planning" class="header-cta" @click="closeMenus">
            <span class="cta-shine" aria-hidden="true" />
            开始规划
          </RouterLink>
        </nav>
      </div>
    </header>

    <main class="site-main">
      <PageTransition />
    </main>

    <footer class="site-footer">
      <div class="site-footer-inner">
        <div>
          <strong>Travel Mind</strong>
          <p>把灵感变成可执行的行程，像整理行李箱一样自然。</p>
        </div>
        <div class="footer-links">
          <RouterLink to="/planning">规划行程</RouterLink>
          <RouterLink to="/map">立体地图</RouterLink>
          <RouterLink to="/trip-history">我的行程</RouterLink>
          <RouterLink to="/ai-lab">AI 灵感</RouterLink>
          <RouterLink to="/profile">旅行偏好</RouterLink>
        </div>
      </div>
    </footer>
  </div>
</template>
