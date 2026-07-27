<script setup>
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import PageTransition from '../components/PageTransition.vue';
import { chapterFor } from './menu.js';
import { useMagnetic } from '../composables/useMagnetic.js';
import { useReveal } from '../composables/useReveal.js';
import { useHudMetrics } from '../composables/useHudMetrics.js';
import { authApi } from '../api/auth.js';
import { authSession } from '../auth/session.js';
import {
  Bot,
  Building2,
  Compass,
  Database,
  ExternalLink,
  Hotel,
  LayoutDashboard,
  LogOut,
  MapPinned,
  Menu,
  NotebookText,
  Route,
  Settings,
  Tags,
  Utensils,
  UsersRound,
  X,
} from 'lucide-vue-next';

const route = useRoute();
const router = useRouter();
const user = computed(() => authSession.user());
const menuOpen = ref(false);
const { clock, scrollProgress } = useHudMetrics();

// 章节码与帘幕换页共用 menu.js 的 chapterMap，管理册走 A 系编号
const chapter = computed(() => chapterFor(route.name));
const chapterCode = computed(() => `${chapter.value[0]} · ${chapter.value[1]}`);

// 体验层：磁吸交互 + 布局级 data-reveal（如底部字标）兑底观察
useMagnetic();
useReveal();
const groups = [
  { label: '总览', items: [{ path: '/admin', label: '运营总览', icon: LayoutDashboard }] },
  {
    label: '内容资源',
    items: [
      { path: '/admin/resources/cities', label: '城市', icon: Building2 },
      { path: '/admin/resources/attractions', label: '景点', icon: Compass },
      { path: '/admin/resources/hotels', label: '住宿', icon: Hotel },
      { path: '/admin/resources/restaurants', label: '餐饮', icon: Utensils },
      { path: '/admin/resources/map-pois', label: '地图地点库', icon: MapPinned },
      { path: '/admin/resources/travel-tags', label: '标签', icon: Tags },
    ],
  },
  {
    label: '用户与业务',
    items: [
      { path: '/admin/resources/users', label: '用户', icon: UsersRound },
      { path: '/admin/resources/user-preferences', label: '用户偏好', icon: Settings },
      { path: '/admin/resources/trip-plans', label: '行程', icon: Route },
      { path: '/admin/resources/travel-notes', label: '笔记', icon: NotebookText },
      { path: '/admin/resources/ai-records', label: 'AI 记录', icon: Bot },
    ],
  },
  { label: '系统', items: [{ path: '/admin/settings', label: '运行配置', icon: Settings }, { path: '/admin/ai-tools', label: 'AI 工具验证', icon: Database }] },
];

function active(path) {
  return path === '/admin' ? route.path === path : route.path.startsWith(path);
}

async function logout() {
  await authApi.logout('admin');
  router.replace('/admin/login');
}

watch(() => route.fullPath, () => { menuOpen.value = false; });
</script>

<template>
  <div class="admin-shell">
    <div class="app-atmosphere" aria-hidden="true">
      <div class="app-atmosphere-grid" />
      <div class="app-atmosphere-code">
        <span class="app-atmosphere-code-label">TM · OPS</span>
        <span class="app-atmosphere-code-value">{{ chapterCode }}</span>
      </div>
    </div>
    <button v-if="menuOpen" type="button" class="admin-sidebar-scrim" aria-label="关闭管理菜单" @click="menuOpen = false" />
    <aside id="admin-navigation" class="admin-sidebar" :class="{ 'is-open': menuOpen }">
      <div class="admin-sidebar-head">
        <RouterLink to="/admin" class="admin-brand">
          <span class="admin-brand-mark" aria-hidden="true">
            <Compass :size="18" :stroke-width="2.2" />
          </span>
          <span class="admin-brand-text">
            <strong>Travel Mind</strong>
            <span>运营管理中心</span>
          </span>
        </RouterLink>
        <button type="button" class="admin-sidebar-close" aria-label="关闭管理菜单" @click="menuOpen = false"><X :size="19" /></button>
      </div>
      <nav aria-label="管理导航">
        <section v-for="group in groups" :key="group.label" class="admin-nav-group">
          <h2>{{ group.label }}</h2>
          <RouterLink
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            :class="{ 'is-active': active(item.path) }"
          ><component :is="item.icon" class="admin-nav-icon" :size="16" aria-hidden="true" />{{ item.label }}</RouterLink>
        </section>
      </nav>
      <div class="admin-account">
        <div><strong>{{ user?.name || '管理员' }}</strong><span>管理员账号</span></div>
        <button type="button" aria-label="退出管理账号" @click="logout"><LogOut :size="16" aria-hidden="true" /></button>
      </div>
    </aside>
    <div class="admin-workspace">
      <header class="admin-topbar">
        <div class="admin-topbar-progress" aria-hidden="true"><i :style="{ '--scroll-progress': scrollProgress }" /></div>
        <button
          type="button"
          class="admin-mobile-toggle"
          aria-label="打开管理菜单"
          aria-controls="admin-navigation"
          :aria-expanded="menuOpen"
          @click="menuOpen = true"
        ><Menu :size="19" /></button>
        <div class="admin-topbar-title">
          <span class="admin-topbar-kicker">OPS</span>
          <strong>{{ route.meta.title || '运营工作台' }}</strong>
        </div>
        <div class="admin-topbar-meta">
          <span class="admin-topbar-clock" aria-hidden="true">{{ clock }} · UTC+8</span>
          <span class="admin-topbar-pulse" aria-hidden="true" />
          <span class="admin-topbar-status">系统在线</span>
          <RouterLink to="/" target="_blank" rel="noopener" class="admin-topbar-link" data-magnetic>用户端 <ExternalLink :size="14" aria-hidden="true" /></RouterLink>
        </div>
      </header>
      <main class="admin-main"><PageTransition /></main>
      <!-- 视口级轮廓字标：与用户端页脚同源的空间锚点，滚到底部从地平线升起 -->
      <div class="footer-wordmark admin-wordmark" data-reveal aria-hidden="true">
        <span>TM · OPS DECK</span>
      </div>
    </div>
  </div>
</template>
