<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { Plane, ChevronDown, LogOut, Menu, X, Compass, Sparkles, Map, BookOpen, BookMarked, Heart, NotebookPen, ScanText, Footprints, SlidersHorizontal, Bell } from 'lucide-vue-next';
import { accountNav, primaryNav, chapterFor } from './menu.js';
import PageTransition from '../components/PageTransition.vue';
import InspirationBagFloat from '../components/InspirationBagFloat.vue';
import { useMagnetic } from '../composables/useMagnetic.js';
import { useReveal } from '../composables/useReveal.js';
import { useHudMetrics } from '../composables/useHudMetrics.js';
import { authApi } from '../api/auth.js';
import { authSession } from '../auth/session.js';

const accountIcon = {
  '/notifications': Bell,
  '/memories': BookMarked,
  '/inspiration-bag': Sparkles,
  '/my-posts': BookOpen,
  '/favorites': Heart,
  '/travel-notes': NotebookPen,
  '/ai-lab': ScanText,
  '/ai-records': Footprints,
  '/profile': SlidersHorizontal,
};

const route = useRoute();
const router = useRouter();
const currentUser = ref(authSession.user());
const userInitial = computed(() => String(currentUser.value?.name || '旅').trim().charAt(0));
const menuOpen = ref(false);
const accountOpen = ref(false);
const { clock, scrolled, scrollProgress } = useHudMetrics();
const showInspirationBag = computed(() => currentUser.value && !/^\/memories(?:\/|$)/.test(route.path));

// 跨页面章节码：与帘幕换页共用 menu.js 的 chapterMap，形成"连续手记"叙事感
const chapter = computed(() => chapterFor(route.name));
const chapterCode = computed(() => `${chapter.value[0]} · ${chapter.value[1]}`);

// 磁吸交互：头部 CTA、账户触发器等带 data-magnetic 的元素
useMagnetic();
// 布局层兜底 reveal：视图根之外的 data-reveal（如页脚字标）由 document 根观察
useReveal();

function closeMenus() {
  menuOpen.value = false;
  accountOpen.value = false;
}

function onDocClick(e) {
  if (!e.target.closest?.('.account-menu') && !e.target.closest?.('.nav-toggle')) {
    accountOpen.value = false;
  }
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
});

onUnmounted(() => {
  document.removeEventListener('click', onDocClick);
});
</script>

<template>
  <div class="app-shell">
    <!-- 跨页面连续叙事层：固定坐标网格，让翻页感觉是同一块画布 -->
    <div class="app-atmosphere" aria-hidden="true">
      <div class="app-atmosphere-grid" />
    </div>

    <!-- HUD 取景框仪表：四角坐标刻度 + 滚动光谱进度线 -->
    <div class="hud-frame" aria-hidden="true">
      <div class="hud-progress"><i :style="{ '--scroll-progress': scrollProgress }" /></div>
      <span class="hud-corner hud-corner--tl">TM / FIELD NOTES</span>
      <span class="hud-corner hud-corner--tr">{{ clock }} · UTC+8</span>
      <span class="hud-corner hud-corner--bl">CHAPTER <b>{{ chapterCode }}</b></span>
      <span class="hud-corner hud-corner--br">SCROLL <b>{{ Math.round(scrollProgress * 100) }}%</b></span>
    </div>

    <header class="site-header" :class="{ 'is-scrolled': scrolled }">
      <div class="site-header-inner">
        <RouterLink to="/" class="brand" @click="closeMenus">
          <span class="brand-mark" aria-hidden="true">
            <Plane class="brand-plane" :size="20" :stroke-width="2.2" />
          </span>
          <span class="brand-text">
            <strong>Travel Mind</strong>
            <small>把下一站写清楚</small>
          </span>
        </RouterLink>

        <button
          type="button"
          class="nav-toggle"
          :aria-expanded="menuOpen"
          :aria-label="menuOpen ? '关闭菜单' : '打开菜单'"
          @click="menuOpen = !menuOpen"
        >
          <Menu v-if="!menuOpen" :size="20" />
          <X v-else :size="20" />
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

          <RouterLink to="/map" class="header-cta btn-fluid" data-magnetic @click="closeMenus">
            <Compass :size="16" :stroke-width="2.2" />
            <span class="cta-shine" aria-hidden="true" />
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
              <span class="account-chevron" aria-hidden="true"><ChevronDown :size="15" /></span>
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
                <component :is="accountIcon[item.path] || Map" :size="15" :stroke-width="2" />
                {{ item.label }}
              </RouterLink>
              <button type="button" class="account-logout" role="menuitem" @click="logout">
                <LogOut :size="15" :stroke-width="2" />
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
      <!-- 视口级轮廓字标：滚动到底时从地平线升起 -->
      <div class="footer-wordmark" data-reveal aria-hidden="true">
        <span>TRAVEL MIND</span>
      </div>
      <div class="site-footer-inner">
        <div>
          <strong>Travel Mind</strong>
          <p>从一条灵感，到一份走得通的行程。</p>
        </div>
        <div class="footer-links">
          <RouterLink to="/map">生成行程</RouterLink>
          <RouterLink to="/map">地图情报</RouterLink>
          <RouterLink to="/trip-history">我的行程</RouterLink>
          <RouterLink to="/cities">发现城市</RouterLink>
          <RouterLink to="/assistant">先问 AI</RouterLink>
        </div>
        <div class="footer-disclosures">
          <details id="service-terms"><summary>服务边界与用户协议</summary><p>Travel Mind 提供旅行信息整理和 AI 规划建议，不直接完成酒店、门票或交通预订。价格、库存、营业和预约要求以实际服务方为准。使用规划结果前，请根据自身健康、天气和当地规定作出判断。</p></details>
          <details id="privacy-notice"><summary>隐私与内容公开说明</summary><p>账号信息、行程、偏好和你主动上传的内容用于提供规划与回忆功能。旅行记录默认仅自己可见；只有在你确认发布并通过审核后，内容才会进入社区。演示环境请勿上传证件、支付凭证等敏感信息。你可以在偏好页导出个人数据或停用账号。</p></details>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* 主内容位于氛围层之上；页头和浮层沿用全局层级，避免被正文覆盖。 */
.app-shell { position: relative; z-index: 1; }
.site-main,
.site-footer { position: relative; z-index: 2; }
</style>
