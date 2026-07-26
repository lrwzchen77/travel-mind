<script setup>
import { ref } from 'vue';
import { RouterLink } from 'vue-router';
import PagePrologue from '../components/PagePrologue.vue';
import { useReveal } from '../composables/useReveal.js';
import {
  ArrowRight,
  BrainCircuit,
  History,
  Sparkles,
  Wand2,
} from 'lucide-vue-next';

const root = ref(null);
useReveal(root);

const aiEntries = [
  {
    to: '/ai-lab',
    icon: Wand2,
    kicker: 'Tool · 01',
    title: '灵感实验台',
    desc: '把模糊的念头丢进实验台，让模型把它折成具体的城市、景点与节奏。',
  },
  {
    to: '/ai-records',
    icon: History,
    kicker: 'Tool · 02',
    title: '分析足迹',
    desc: '回看每一次 AI 排程的输入与产出，理解模型的偏好，也理解自己。',
  },
  {
    to: '/inspirations',
    icon: Sparkles,
    kicker: 'Tool · 03',
    title: '社区灵感流',
    desc: '从别人的真实行程里采撷碎片，拼成下一程的起点。',
  },
  {
    to: '/assistant',
    icon: BrainCircuit,
    kicker: 'Tool · 04',
    title: '旅行助手',
    desc: '一个会追问、会修正、会算时间的同行者，随时进入下一段对话。',
  },
];
</script>

<template>
  <div ref="root" class="ai-atelier">
    <PagePrologue
      index="09 · 助手"
      eyebrow="AI Atelier"
      title="让灵感落地"
      lead="Travel Mind 的 AI 模块不是问答机，而是一间工作室：输入模糊，输出可执行。从一条线索开始，逐步折叠成可走的行程。"
      next-label="进入灵感实验台"
      next-to="/ai-lab"
    />

    <section class="ai-grid" aria-label="AI 工具入口">
      <RouterLink
        v-for="(entry, i) in aiEntries"
        :key="entry.to"
        :to="entry.to"
        class="ai-card"
        data-reveal
        :style="{ '--reveal-delay': `${0.08 * i + 0.05}s` }"
      >
        <div class="ai-card-rail" aria-hidden="true">
          <span class="ai-card-index">{{ String(i + 1).padStart(2, '0') }}</span>
          <span class="ai-card-rule" />
        </div>
        <div class="ai-card-body">
          <p class="ai-card-kicker">{{ entry.kicker }}</p>
          <h3 class="ai-card-title">{{ entry.title }}</h3>
          <p class="ai-card-desc">{{ entry.desc }}</p>
        </div>
        <div class="ai-card-foot">
          <component :is="entry.icon" :size="18" :stroke-width="2" aria-hidden="true" />
          <span class="ai-card-cta">进入</span>
          <ArrowRight :size="14" :stroke-width="2.2" aria-hidden="true" />
        </div>
        <span class="ai-card-glow" aria-hidden="true" />
      </RouterLink>
    </section>

    <section class="chapter-bridge" data-reveal>
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章</p>
        <h2 class="chapter-bridge-title">把灵感折成行程</h2>
        <p class="chapter-bridge-lead">实验台给出的方案，最终都要落到地图上。打开规划器，让一次灵感变成可走的路线。</p>
      </div>
      <RouterLink class="chapter-bridge-cta" to="/map">
        <span>去地图规划</span>
        <ArrowRight :size="18" :stroke-width="2.2" />
      </RouterLink>
    </section>
  </div>
</template>

<style scoped>
.ai-atelier {
  position: relative;
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 0 80px;
}

/* ── AI 工具入口网格 ── */
.ai-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 8px;
}

.ai-card {
  position: relative;
  display: grid;
  grid-template-columns: 64px 1fr;
  grid-template-rows: 1fr auto;
  gap: 4px 0;
  padding: 28px 28px 22px;
  border: 1px solid var(--tm-line);
  border-radius: var(--tm-radius-panel);
  background: linear-gradient(155deg, var(--tm-paper-muted) 0%, var(--tm-paper) 100%);
  overflow: hidden;
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1),
              border-color 0.4s ease,
              box-shadow 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}
.ai-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--tm-accent), transparent);
  opacity: 0;
  transition: opacity 0.4s ease;
}
.ai-card:hover {
  transform: translateY(-4px);
  border-color: var(--tm-accent-soft);
  box-shadow: 0 24px 60px -30px rgba(0, 0, 0, 0.8),
              0 0 0 1px var(--tm-accent-soft);
}
.ai-card:hover::before { opacity: 1; }

.ai-card-rail {
  grid-column: 1;
  grid-row: 1 / span 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding-top: 4px;
}
.ai-card-index {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--tm-accent);
}
.ai-card-rule {
  flex: 1;
  width: 1px;
  min-height: 80px;
  background: linear-gradient(180deg, var(--tm-accent), transparent);
  opacity: 0.5;
}

.ai-card-body {
  grid-column: 2;
  display: grid;
  gap: 8px;
  padding-bottom: 14px;
}
.ai-card-kicker {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--tm-muted);
}
.ai-card-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(22px, 2.4vw, 28px);
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.15;
  color: var(--tm-ink);
}
.ai-card-desc {
  margin: 4px 0 0;
  max-width: 46ch;
  font-size: 14px;
  line-height: 1.65;
  color: var(--tm-ink-soft);
}

.ai-card-foot {
  grid-column: 2;
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 14px;
  border-top: 1px solid var(--tm-line-soft);
  color: var(--tm-accent);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  transition: gap 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.ai-card:hover .ai-card-foot { gap: 16px; }
.ai-card-cta { flex: 1; }

/* 卡片入场动效（由 useReveal 触发） */
.ai-card {
  opacity: 0;
  transform: translateY(18px);
}
.ai-card.is-inview {
  animation: ai-card-in 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  animation-delay: var(--reveal-delay, 0ms);
}
@keyframes ai-card-in {
  to { opacity: 1; transform: translateY(0); }
}

/* ── 装饰光晕：卡片右上角持续呼吸 ── */
.ai-card-glow {
  position: absolute;
  top: -40px;
  right: -40px;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--tm-accent-glow) 0%, transparent 70%);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.5s ease;
}
.ai-card:hover .ai-card-glow { opacity: 0.4; }

/* ── 响应式 ── */
@media (max-width: 860px) {
  .ai-grid { grid-template-columns: 1fr; }
  .ai-card { grid-template-columns: 48px 1fr; padding: 22px 22px 18px; }
}
</style>
