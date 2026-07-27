<script setup>
/**
 * SplitText — 字符级动力学排版
 * 把一段文字拆成逐字符 span，按序赋予 --char-index，
 * 由 CSS 弹性缓动逐字上升入场；中英文混排均可。
 */
import { computed } from 'vue';

const props = defineProps({
  text: { type: String, required: true },
  tag: { type: String, default: 'span' },
  /** 每个字符的级联延迟（ms） */
  stagger: { type: Number, default: 34 },
  /** 首字符起始延迟（ms） */
  delay: { type: Number, default: 0 },
});

const chars = computed(() => Array.from(props.text));
</script>

<template>
  <component :is="tag" class="split-text" :aria-label="text">
    <span
      v-for="(ch, i) in chars"
      :key="`${ch}-${i}`"
      class="split-char"
      aria-hidden="true"
      :style="{ '--char-delay': `${delay + i * stagger}ms` }"
    >{{ ch === ' ' ? '\u00A0' : ch }}</span>
  </component>
</template>

<style>
.split-text { display: inline-block; }
.split-char {
  display: inline-block;
  transform-origin: 50% 100%;
  animation: split-char-rise 0.9s cubic-bezier(0.22, 1.4, 0.36, 1) both;
  animation-delay: var(--char-delay, 0ms);
  will-change: transform, opacity;
}
@keyframes split-char-rise {
  from { opacity: 0; transform: translateY(0.9em) rotate(6deg) scaleY(1.2); }
  60% { opacity: 1; }
  to { opacity: 1; transform: translateY(0) rotate(0) scaleY(1); }
}
@media (prefers-reduced-motion: reduce) {
  .split-char { animation: none; }
}
</style>
