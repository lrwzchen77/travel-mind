<script setup>
// 旅途精灵：Travel Mind 的数字 IP。纯 SVG + 代码动效，无位图资源。
// 眼睛以弹簧插值跟随光标，state=thinking 时轨道加速、眯眼、天线快闪。
import { onBeforeUnmount, onMounted, ref } from 'vue';

defineProps({
  state: { type: String, default: 'idle' }, // idle | thinking
  size: { type: Number, default: 120 },
});

// 渐变 id 防多实例冲突
const gid = `sprite-${Math.random().toString(36).slice(2, 8)}`;
const host = ref(null);
const eyes = ref({ x: 0, y: 0 });
let target = { x: 0, y: 0 };
let raf = 0;

function onPointerMove(event) {
  const el = host.value;
  if (!el) return;
  const rect = el.getBoundingClientRect();
  const dx = event.clientX - (rect.left + rect.width / 2);
  const dy = event.clientY - (rect.top + rect.height / 2);
  const distance = Math.hypot(dx, dy) || 1;
  // 瞳孔最大偏移 3.4，越近越收敛
  const reach = Math.min(distance / 70, 1) * 3.4;
  target = { x: (dx / distance) * reach, y: (dy / distance) * reach };
  if (!raf) raf = requestAnimationFrame(loop);
}

function loop() {
  raf = 0;
  const dx = target.x - eyes.value.x;
  const dy = target.y - eyes.value.y;
  eyes.value = { x: eyes.value.x + dx * 0.12, y: eyes.value.y + dy * 0.12 };
  if (Math.abs(dx) > 0.02 || Math.abs(dy) > 0.02) raf = requestAnimationFrame(loop);
}

onMounted(() => {
  const finePointer = window.matchMedia?.('(pointer: fine)')?.matches;
  const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;
  if (!finePointer || reducedMotion) return;
  window.addEventListener('pointermove', onPointerMove, { passive: true });
});

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointerMove);
  cancelAnimationFrame(raf);
});
</script>

<template>
  <span ref="host" class="travel-sprite" :class="`is-${state}`" :style="{ width: `${size}px`, height: `${size}px` }">
    <svg :width="size" :height="size" viewBox="0 0 120 120" fill="none" aria-hidden="true">
      <defs>
        <radialGradient :id="gid" cx="50%" cy="36%" r="78%">
          <stop offset="0%" stop-color="var(--tm-paper-raised, #2c2013)" />
          <stop offset="100%" stop-color="var(--tm-paper-muted, #1d150d)" />
        </radialGradient>
      </defs>

      <!-- 轨道环 + 卫星：待机慢转，思考时加速 -->
      <g class="sprite-orbit">
        <circle cx="60" cy="60" r="54" stroke="var(--tm-line-strong)" stroke-width="1" stroke-dasharray="3 8" />
        <circle cx="60" cy="6" r="3" fill="var(--tm-accent)" />
      </g>

      <!-- 天线 -->
      <line x1="60" y1="20" x2="60" y2="30" stroke="var(--tm-accent)" stroke-width="2" stroke-linecap="round" />
      <circle class="sprite-antenna-tip" cx="60" cy="16" r="3.2" fill="var(--tm-accent)" />

      <!-- 身体 -->
      <circle cx="60" cy="64" r="34" :fill="`url(#${gid})`" stroke="var(--tm-accent)" stroke-width="1.5" />
      <!-- 身体赤道刻度线 -->
      <path d="M28 72 Q60 84 92 72" stroke="var(--tm-line-strong)" stroke-width="1" stroke-dasharray="2 5" />

      <!-- 眼睛：外层跟随光标，内层负责眨眼 -->
      <g :transform="`translate(${eyes.x.toFixed(2)} ${eyes.y.toFixed(2)})`">
        <g class="sprite-eyes-blink">
          <circle cx="48" cy="58" r="5" fill="var(--tm-accent)" />
          <circle cx="72" cy="58" r="5" fill="var(--tm-accent)" />
          <!-- 高光 -->
          <circle cx="49.6" cy="56.4" r="1.4" fill="var(--tm-canvas, #120c07)" />
          <circle cx="73.6" cy="56.4" r="1.4" fill="var(--tm-canvas, #120c07)" />
        </g>
      </g>

      <!-- 脸颊 -->
      <circle cx="39" cy="68" r="2.4" fill="var(--tm-accent)" opacity="0.35" />
      <circle cx="81" cy="68" r="2.4" fill="var(--tm-accent)" opacity="0.35" />

      <!-- 嘴：待机微笑 / 思考时变成小圆 -->
      <path class="sprite-mouth-idle" d="M53 72 Q60 78 67 72" stroke="var(--tm-accent)" stroke-width="2" stroke-linecap="round" />
      <circle class="sprite-mouth-think" cx="60" cy="74" r="3" stroke="var(--tm-accent)" stroke-width="2" />
    </svg>
  </span>
</template>

<style scoped>
.travel-sprite { display: inline-grid; place-items: center; }
.travel-sprite svg { display: block; overflow: visible; animation: sprite-float 5.2s ease-in-out infinite; }

.sprite-orbit {
  transform-origin: 60px 60px;
  animation: sprite-orbit-spin 16s linear infinite;
}

.sprite-antenna-tip {
  transform-origin: 60px 16px;
  animation: sprite-tip-pulse 2.6s ease-in-out infinite;
}

.sprite-eyes-blink {
  transform-origin: 60px 58px;
  animation: sprite-blink 4.6s infinite;
}

.sprite-mouth-think { opacity: 0; }

/* 思考态：轨道加速、眯眼、天线快闪、嘴变小圆 */
.is-thinking .sprite-orbit { animation-duration: 2.4s; }
.is-thinking .sprite-antenna-tip { animation-duration: 0.7s; }
.is-thinking .sprite-eyes-blink { animation: none; transform: scaleY(0.42); }
.is-thinking .sprite-mouth-idle { opacity: 0; }
.is-thinking .sprite-mouth-think { opacity: 1; }

@keyframes sprite-float {
  50% { transform: translateY(-5px); }
}

@keyframes sprite-orbit-spin {
  to { transform: rotate(360deg); }
}

@keyframes sprite-blink {
  0%, 91%, 100% { transform: scaleY(1); }
  94% { transform: scaleY(0.12); }
}

@keyframes sprite-tip-pulse {
  50% { opacity: 0.25; }
}

@media (prefers-reduced-motion: reduce) {
  .travel-sprite svg,
  .sprite-orbit,
  .sprite-antenna-tip,
  .sprite-eyes-blink { animation: none; }
}
</style>
