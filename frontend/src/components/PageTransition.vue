<script setup>
/**
 * PageTransition — 电影级页面切换
 * 用层级化的位移 + 不透明度 + 裁切，让每次跳转像翻页而非硬切。
 * 配合 PagePrologue 的序号语法，整站读起来像连续手记。
 */
import { RouterView } from 'vue-router';
</script>

<template>
  <RouterView v-slot="{ Component, route }">
    <Transition name="page" mode="out-in">
      <div :key="route.path" class="page-transition-shell">
        <component :is="Component" />
      </div>
    </Transition>
  </RouterView>
</template>

<style>
/* 页面级切换：轻微上浮 + 模糊散焦 + 不透明度，营造镜头推拉感 */
.page-enter-active,
.page-leave-active {
  transition:
    opacity 0.5s cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.6s cubic-bezier(0.16, 1, 0.3, 1),
    filter 0.5s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(28px);
  filter: blur(6px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-18px);
  filter: blur(4px);
}

/* 序章与 data-reveal 子元素的串联入场：覆盖切换后再渐次浮现 */
.page-enter-active .page-prologue,
.page-enter-active [data-reveal] {
  animation: page-reveal-up 0.9s cubic-bezier(0.16, 1, 0.3, 1) both;
}
.page-enter-active .page-prologue { animation-delay: 0.08s; }
.page-enter-active [data-reveal]:not(.page-prologue) {
  animation-delay: var(--reveal-delay, 0.18s);
}

@keyframes page-reveal-up {
  from { opacity: 0; transform: translateY(22px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .page-enter-active,
  .page-leave-active { transition: opacity 0.2s ease; transform: none; filter: none; }
  .page-enter-active .page-prologue,
  .page-enter-active [data-reveal] { animation: none; }
}
</style>
