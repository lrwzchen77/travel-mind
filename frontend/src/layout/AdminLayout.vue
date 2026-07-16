<script setup>
import { computed } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import PageTransition from '../components/PageTransition.vue';
import { authApi } from '../api/auth.js';
import { authSession } from '../auth/session.js';

const route = useRoute();
const router = useRouter();
const user = computed(() => authSession.user());
const groups = [
  { label: '总览', items: [{ path: '/admin', label: '运营总览' }] },
  {
    label: '内容资源',
    items: [
      { path: '/admin/resources/cities', label: '城市' },
      { path: '/admin/resources/attractions', label: '景点' },
      { path: '/admin/resources/hotels', label: '住宿' },
      { path: '/admin/resources/restaurants', label: '餐饮' },
      { path: '/admin/resources/travel-tags', label: '标签' },
    ],
  },
  {
    label: '用户与业务',
    items: [
      { path: '/admin/resources/users', label: '用户' },
      { path: '/admin/resources/trip-plans', label: '行程' },
      { path: '/admin/resources/travel-notes', label: '笔记' },
      { path: '/admin/resources/ai-records', label: 'AI 记录' },
    ],
  },
  { label: '系统', items: [{ path: '/admin/settings', label: '运行配置' }, { path: '/admin/ai-tools', label: 'AI 工具验证' }] },
];

function active(path) {
  return path === '/admin' ? route.path === path : route.path.startsWith(path);
}

async function logout() {
  await authApi.logout('admin');
  router.replace('/admin/login');
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <RouterLink to="/admin" class="admin-brand">
        <strong>Travel Mind</strong>
        <span>运营管理中心</span>
      </RouterLink>
      <nav aria-label="管理导航">
        <section v-for="group in groups" :key="group.label" class="admin-nav-group">
          <h2>{{ group.label }}</h2>
          <RouterLink
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            :class="{ 'is-active': active(item.path) }"
          >{{ item.label }}</RouterLink>
        </section>
      </nav>
      <div class="admin-account">
        <div><strong>{{ user?.name || '管理员' }}</strong><span>管理员账号</span></div>
        <button type="button" @click="logout">退出</button>
      </div>
    </aside>
    <div class="admin-workspace">
      <header class="admin-topbar">
        <div><span>管理端</span><strong>{{ route.meta.title || '运营工作台' }}</strong></div>
        <RouterLink to="/" target="_blank">打开用户端</RouterLink>
      </header>
      <main class="admin-main"><PageTransition /></main>
    </div>
  </div>
</template>
