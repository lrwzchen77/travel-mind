<script setup>
/**
 * PageTransition — 电影级页面切换
 * 两层协同：
 * 1. 帘幕扫光：路由切换时暗幕自下而上扫过视口，中央闪现目标章节码；
 * 2. 页面层级化位移 + 模糊散焦，让翻页像镜头推拉而非硬切。
 * 配合 PagePrologue 的序号语法，整站读起来像连续手记。
 */
import { onBeforeUnmount, ref, watch } from 'vue';
import { RouterView, useRoute } from 'vue-router';
import { chapterFor } from '../layout/menu.js';

const route = useRoute();
const sweeping = ref(false);
const chapter = ref(chapterFor(route.name));
let sweepTimer;
let sweepFrame;

// 首屏不扫幕；后续每次路径切换触发一次帘幕
watch(() => route.path, () => {
  chapter.value = chapterFor(route.name);
  sweeping.value = false;
  if (sweepTimer) window.clearTimeout(sweepTimer);
  if (sweepFrame) window.cancelAnimationFrame(sweepFrame);
  // 下一帧重新挂 is-sweep，保证动画可重复触发
  sweepFrame = window.requestAnimationFrame(() => {
    sweepFrame = 0;
    sweeping.value = true;
    sweepTimer = window.setTimeout(() => { sweeping.value = false; }, 950);
  });
});

onBeforeUnmount(() => {
  if (sweepFrame) window.cancelAnimationFrame(sweepFrame);
  if (sweepTimer) window.clearTimeout(sweepTimer);
});
</script>

<template>
  <div class="page-curtain" :class="{ 'is-sweep': sweeping }" aria-hidden="true">
    <div class="page-curtain-code">
      <b>{{ chapter[0] }}</b>
      <span>{{ chapter[1] }}</span>
    </div>
  </div>

  <RouterView v-slot="{ Component, route: viewRoute }">
    <Transition name="page" mode="out-in">
      <div :key="viewRoute.path" class="page-transition-shell">
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
