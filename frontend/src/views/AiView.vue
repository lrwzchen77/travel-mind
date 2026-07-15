<script setup>
import { RouterLink } from 'vue-router';
</script>

<template>
  <section class="page-intro">
    <p class="eyebrow">AI 灵感</p>
    <h1>让灵感落地</h1>
    <p>去灵感工具箱试跑，或回看你留下的分析足迹。</p>
  </section>
  <div class="hero-actions">
    <RouterLink class="btn-link btn-coral" to="/ai-lab">打开灵感工具</RouterLink>
    <RouterLink class="btn-link btn-ghost" to="/ai-records">分析足迹</RouterLink>
  </div>
</template>
