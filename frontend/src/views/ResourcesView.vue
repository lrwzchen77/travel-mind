<script setup>
import { ref } from 'vue';
import { RouterLink } from 'vue-router';
import PagePrologue from '../components/PagePrologue.vue';
import { useReveal } from '../composables/useReveal.js';
import {
  ArrowRight,
  ArrowUpRight,
  Building2,
  Compass,
  Hotel,
  Utensils,
} from 'lucide-vue-next';

const root = ref(null);
useReveal(root);

const destEntries = [
  {
    to: '/cities',
    icon: Building2,
    kicker: 'Atlas · 01',
    title: '城市',
    desc: '从一座城开始：街区、气候、季节、节奏，所有规划的起点。',
    mood: 'mood-haze',
  },
  {
    to: '/attractions',
    icon: Compass,
    kicker: 'Atlas · 02',
    title: '景点',
    desc: '把想去的点钉在地图上，让路线自己浮出来。',
    mood: 'mood-terra',
  },
  {
    to: '/hotels',
    icon: Hotel,
    kicker: 'Atlas · 03',
    title: '住宿',
    desc: '选一处合心的落脚点，决定一整天的体感与情绪。',
    mood: 'mood-spice',
  },
  {
    to: '/restaurants',
    icon: Utensils,
    kicker: 'Atlas · 04',
    title: '美食',
    desc: '一口地道的风物，常常比景点更记得住一座城。',
    mood: 'mood-sea',
  },
];
</script>

<template>
  <div ref="root" class="resources-atelier">
    <PagePrologue
      index="04 · 城市"
      eyebrow="Discover"
      title="想去的都在这"
      lead="把散落的灵感收进同一本地图册。城市、景点、住宿、美食——四个入口，通往同一次完整的出行。"
      next-label="从城市开始"
      next-to="/cities"
    />

    <section class="dest-grid" aria-label="资源入口">
      <RouterLink
        v-for="(entry, i) in destEntries"
        :key="entry.to"
        :to="entry.to"
        class="dest-card"
        data-reveal
        :style="{ '--reveal-delay': `${0.08 * i + 0.05}s` }"
      >
        <div class="dest-cover" :class="entry.mood">
          <component :is="entry.icon" :size="22" :stroke-width="1.8" class="dest-cover-icon" aria-hidden="true" />
          <strong>{{ entry.title }}</strong>
          <span class="dest-cover-kicker">{{ entry.kicker }}</span>
        </div>
        <div class="dest-body">
          <p class="dest-desc">{{ entry.desc }}</p>
          <div class="dest-cta">
            <span>探索</span>
            <ArrowUpRight :size="14" :stroke-width="2.2" aria-hidden="true" />
          </div>
        </div>
        <span class="dest-glow" aria-hidden="true" />
      </RouterLink>
    </section>

    <section class="chapter-bridge" data-reveal>
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章</p>
        <h2 class="chapter-bridge-title">挑完城市，去地图圈地点</h2>
        <p class="chapter-bridge-lead">资源页负责"想去的"，地图负责"走得通的"。打开立体地图，把刚才收集的点串成线。</p>
      </div>
      <RouterLink class="chapter-bridge-cta" to="/map">
        <span>打开立体地图</span>
        <ArrowRight :size="18" :stroke-width="2.2" />
      </RouterLink>
    </section>
  </div>
</template>

<style scoped>
.resources-atelier {
  position: relative;
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 0 80px;
}

/* ── 资源入口网格 ── */
.dest-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.dest-card {
  position: relative;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--tm-line);
  border-radius: var(--tm-radius-panel);
  background: var(--tm-paper);
  overflow: hidden;
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1),
              border-color 0.4s ease,
              box-shadow 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}
.dest-card:hover {
  transform: translateY(-4px);
  border-color: var(--tm-accent-soft);
  box-shadow: 0 24px 60px -30px rgba(0, 0, 0, 0.8),
              0 0 0 1px var(--tm-accent-soft);
}

.dest-cover {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 6px;
  min-height: 180px;
  padding: 22px;
  overflow: hidden;
}
.dest-cover::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 30%, rgba(12, 10, 8, 0.78) 100%);
  z-index: 1;
}
.dest-cover-icon {
  position: absolute;
  top: 22px;
  right: 22px;
  z-index: 2;
  color: rgba(255, 255, 255, 0.85);
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.dest-card:hover .dest-cover-icon {
  transform: rotate(-8deg) scale(1.15);
}
.dest-cover strong {
  position: relative;
  z-index: 2;
  font-family: var(--font-display);
  font-size: clamp(26px, 2.6vw, 34px);
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.05;
  color: #fffdf8;
}
.dest-cover-kicker {
  position: relative;
  z-index: 2;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.7);
}

/* 各入口的封面渐变（暗色基底 + 暖色点缀） */
.dest-cover.mood-haze {
  background: linear-gradient(150deg, #2a2620 0%, #1a1611 60%, #4a3d2e 100%);
}
.dest-cover.mood-terra {
  background: linear-gradient(150deg, #3a2820 0%, #1a1611 60%, #ff7a3d 130%);
}
.dest-cover.mood-spice {
  background: linear-gradient(150deg, #2e2618 0%, #1a1611 60%, #ffc857 130%);
}
.dest-cover.mood-sea {
  background: linear-gradient(150deg, #1e2a2a 0%, #1a1611 60%, #5fb6a8 130%);
}

.dest-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 22px 22px;
  flex: 1;
}
.dest-desc {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.65;
  color: var(--tm-ink-soft);
}
.dest-cta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--tm-line-soft);
  color: var(--tm-accent);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  transition: gap 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.dest-card:hover .dest-cta { gap: 14px; }

/* 卡片入场动效 */
.dest-card {
  opacity: 0;
  transform: translateY(20px);
}
.dest-card.is-inview {
  animation: dest-card-in 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  animation-delay: var(--reveal-delay, 0ms);
}
@keyframes dest-card-in {
  to { opacity: 1; transform: translateY(0); }
}

/* 装饰光晕 */
.dest-glow {
  position: absolute;
  top: -50px;
  right: -50px;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--tm-accent-glow) 0%, transparent 70%);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.5s ease;
}
.dest-card:hover .dest-glow { opacity: 0.5; }

/* ── 响应式 ── */
@media (max-width: 1024px) {
  .dest-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 600px) {
  .dest-grid { grid-template-columns: 1fr; }
}
</style>
