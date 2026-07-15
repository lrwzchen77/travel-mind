<script setup>
import { RouterLink } from 'vue-router';
</script>

<template>
  <section class="page-intro">
    <p class="eyebrow">发现</p>
    <h1>想去的都在这</h1>
    <p>城市、景点、住与吃——先逛一圈，再带进规划。</p>
  </section>
  <div class="dest-grid" style="grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));">
    <RouterLink to="/cities" class="dest-card">
      <div class="dest-cover mood-haze"><strong>城市</strong></div>
      <div class="dest-body"><p>发现下一站目的地</p></div>
    </RouterLink>
    <RouterLink to="/attractions" class="dest-card">
      <div class="dest-cover mood-terra"><strong>景点</strong></div>
      <div class="dest-body"><p>想打卡的都列上</p></div>
    </RouterLink>
    <RouterLink to="/hotels" class="dest-card">
      <div class="dest-cover mood-spice"><strong>住宿</strong></div>
      <div class="dest-body"><p>睡得好，玩才尽兴</p></div>
    </RouterLink>
    <RouterLink to="/restaurants" class="dest-card">
      <div class="dest-cover mood-sea"><strong>美食</strong></div>
      <div class="dest-body"><p>本地味道不踩雷</p></div>
    </RouterLink>
  </div>
</template>
