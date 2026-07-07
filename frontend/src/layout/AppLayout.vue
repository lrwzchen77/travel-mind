<script setup>
import { RouterLink, RouterView } from 'vue-router';
import { navigationItems } from './menu.js';
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">TM</span>
        <div>
          <strong>Travel Mind</strong>
          <small>Planning Workspace</small>
        </div>
      </div>

      <nav class="main-nav" aria-label="Main navigation">
        <RouterLink
          v-for="item in navigationItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
        >
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>
