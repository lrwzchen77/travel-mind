<script setup>
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import PageTransition from '../components/PageTransition.vue';
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
const chapterMap = {
  'admin-dashboard': '00 · 运营',
  'admin-settings': '01 · 配置',
  'admin-ai-tools': '02 · 工具',
};
const chapterCode = computed(() => chapterMap[route.name] || '02 · 资源');
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
          <span class="admin-topbar-pulse" aria-hidden="true" />
          <span class="admin-topbar-status">系统在线</span>
          <RouterLink to="/" target="_blank" class="admin-topbar-link">用户端 <ExternalLink :size="14" aria-hidden="true" /></RouterLink>
        </div>
      </header>
      <main class="admin-main"><PageTransition /></main>
    </div>
  </div>
</template>
