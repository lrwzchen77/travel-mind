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

<style scoped>
/* ── Admin shell: cinematic operations console ── */
.admin-shell {
  position: relative;
}
.admin-shell::before {
  content: "";
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    linear-gradient(rgba(243, 235, 220, 0.018) 1px, transparent 1px),
    linear-gradient(90deg, rgba(243, 235, 220, 0.018) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: radial-gradient(120% 100% at 70% 30%, #000 20%, transparent 80%);
}
.admin-sidebar,
.admin-workspace { position: relative; z-index: 1; }

/* ── Sidebar: editorial group headers + magnetic active rail ── */
.admin-sidebar {
  border-right: 1px solid var(--tm-line);
  background:
    linear-gradient(180deg, rgba(21, 17, 12, 0.96) 0%, rgba(14, 11, 8, 0.98) 100%),
    radial-gradient(120% 80% at 0% 0%, var(--tm-accent-soft) 0%, transparent 45%);
  position: relative;
}
.admin-sidebar::after {
  content: "";
  position: absolute;
  top: 0;
  right: 0;
  width: 1px;
  height: 100%;
  background: linear-gradient(180deg, transparent 0%, var(--tm-line) 18%, var(--tm-line) 82%, transparent 100%);
  pointer-events: none;
}
.admin-sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--tm-line-soft);
  margin-bottom: 14px;
}
.admin-brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0;
  position: relative;
}
.admin-brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, var(--tm-accent) 0%, var(--tm-accent-deep) 100%);
  color: #160d05;
  box-shadow: 0 6px 18px -8px var(--tm-accent-glow), inset 0 1px 0 rgba(255, 255, 255, 0.28);
  flex-shrink: 0;
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.admin-brand:hover .admin-brand-mark { transform: rotate(-8deg) scale(1.04); }
.admin-brand-text { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.admin-brand strong {
  font-family: var(--font-display);
  font-size: 16.5px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--tm-ink);
  line-height: 1.1;
}
.admin-brand-text span {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 500;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: var(--tm-muted);
  line-height: 1;
}

.admin-nav-group {
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--tm-line-soft);
}
.admin-nav-group:last-of-type {
  border-bottom: 0;
  margin-bottom: 8px;
  padding-bottom: 0;
}
.admin-nav-group h2 {
  position: relative;
  margin: 0 10px 8px;
  padding-left: 14px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--tm-muted);
}
.admin-nav-group h2::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 8px;
  height: 1px;
  background: var(--tm-accent);
  transform: translateY(-50%);
}
.admin-nav-group a {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 9px 12px;
  border-radius: var(--tm-radius-control);
  color: var(--tm-ink-soft);
  font-size: 13px;
  font-weight: 500;
  transition: background 0.25s ease, color 0.25s ease, padding-left 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.admin-nav-group a:hover {
  color: var(--tm-ink);
  background: var(--tm-paper-raised);
}
.admin-nav-group a.is-active {
  color: var(--tm-accent);
  background: var(--tm-accent-soft);
  padding-left: 16px;
  font-weight: 600;
}
.admin-nav-group a.is-active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 18px;
  border-radius: 2px;
  background: var(--tm-accent);
  transform: translateY(-50%);
}
.admin-nav-icon { flex-shrink: 0; }

/* ── Account footer ── */
.admin-account {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding: 14px 10px 4px;
  border-top: 1px solid var(--tm-line);
}
.admin-account div { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.admin-account strong {
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  color: var(--tm-ink);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.admin-account span {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--tm-muted);
}
.admin-account button {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: var(--tm-radius-control);
  background: transparent;
  color: var(--tm-muted);
  cursor: pointer;
  transition: all 0.25s ease;
}
.admin-account button:hover {
  color: var(--tm-danger);
  background: var(--tm-danger-soft);
  border-color: var(--tm-danger-soft);
}

/* ── Topbar: ops console header ── */
.admin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 64px;
  padding: 0 clamp(20px, 3vw, 32px);
  border-bottom: 1px solid var(--tm-line);
  background: rgba(21, 17, 12, 0.72);
  backdrop-filter: blur(20px) saturate(1.4);
}
.admin-topbar-title { display: flex; align-items: baseline; gap: 12px; min-width: 0; }
.admin-topbar-kicker {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.28em;
  color: var(--tm-accent);
}
.admin-topbar-title strong {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--tm-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.admin-topbar-meta { display: flex; align-items: center; gap: 14px; }
.admin-topbar-pulse {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--tm-success);
  box-shadow: 0 0 10px var(--tm-success);
  animation: admin-pulse 2.4s ease-in-out infinite;
}
@keyframes admin-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.85); }
}
.admin-topbar-status {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.12em;
  color: var(--tm-muted);
}
.admin-topbar-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: 1px solid var(--tm-line-strong);
  border-radius: var(--tm-radius-pill);
  color: var(--tm-ink-soft);
  font-size: 12px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.25s ease;
}
.admin-topbar-link:hover {
  border-color: var(--tm-accent);
  color: var(--tm-accent);
  background: var(--tm-accent-soft);
}
.admin-mobile-toggle,
.admin-sidebar-close {
  display: none;
  width: 42px;
  height: 42px;
  place-items: center;
  padding: 0;
  border: 1px solid var(--tm-line-strong);
  border-radius: var(--tm-radius-button);
  color: var(--tm-ink);
  background: transparent;
  cursor: pointer;
}

@media (max-width: 980px) {
  .admin-mobile-toggle { display: grid; }
  .admin-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 60;
    width: 280px;
    height: 100vh;
    transform: translateX(-102%);
    transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  }
  .admin-sidebar.is-open { transform: translateX(0); }
  .admin-sidebar-close { display: grid; }
  .admin-topbar-status { display: none; }
}
</style>
