<script setup>
/**
 * PagePrologue — 统一的页面序章
 * 把每个子页面纳入同一套编辑叙事：序号 · 眉标 · 展示型标题 · 引子。
 * 跨页面共享这套语法后，整个应用读起来像一本连续的旅行手记，
 * 而不是一组互不相干的功能页。
 */
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { ArrowRight } from 'lucide-vue-next';

const props = defineProps({
  index: { type: String, default: '' },
  eyebrow: { type: String, default: '' },
  title: { type: String, required: true },
  lead: { type: String, default: '' },
  nextLabel: { type: String, default: '' },
  nextTo: { type: String, default: '' },
  align: { type: String, default: 'start' },
});

const route = useRoute();
const routeName = computed(() => String(route.name || ''));
const stamp = computed(() => props.index || routeName.value.slice(0, 3).toUpperCase());
</script>

<template>
  <header class="page-prologue" :class="`is-${align}`">
    <div class="page-prologue-rail" aria-hidden="true">
      <span class="page-prologue-stamp">{{ stamp }}</span>
      <span class="page-prologue-rule" />
      <span class="page-prologue-dot" />
    </div>

    <p v-if="eyebrow" class="page-prologue-eyebrow">{{ eyebrow }}</p>

    <h1 class="page-prologue-title">
      <slot name="title">{{ title }}</slot>
    </h1>

    <p v-if="lead || $slots.lead" class="page-prologue-lead">
      <slot name="lead">{{ lead }}</slot>
    </p>

    <div v-if="nextLabel && nextTo" class="page-prologue-next">
      <RouterLink :to="nextTo" class="page-prologue-next-link">
        <span>{{ nextLabel }}</span>
        <ArrowRight :size="16" :stroke-width="2.2" />
      </RouterLink>
    </div>
  </header>
</template>

<style scoped>
.page-prologue {
  position: relative;
  max-width: 880px;
  margin: 0 0 clamp(36px, 5vw, 64px);
  padding-top: 6px;
}
.page-prologue.is-center { text-align: center; margin-inline: auto; }

.page-prologue-rail {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 22px;
}
.page-prologue.is-center .page-prologue-rail { justify-content: center; }

.page-prologue-stamp {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.32em;
  color: var(--tm-accent);
  font-weight: 700;
}

.page-prologue-rule {
  flex: 0 0 64px;
  height: 1px;
  background: linear-gradient(90deg, var(--tm-accent), transparent);
}

.page-prologue-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--tm-accent);
  box-shadow: 0 0 14px var(--tm-accent-glow);
}

.page-prologue-eyebrow {
  margin: 0 0 14px;
  color: var(--tm-muted);
  font-family: var(--font-mono);
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.page-prologue-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(40px, 6.4vw, 84px);
  font-weight: 700;
  line-height: 0.98;
  letter-spacing: -0.045em;
  color: var(--tm-ink);
}
.page-prologue-title :deep(em) {
  font-style: normal;
  color: var(--tm-accent);
}

.page-prologue-lead {
  margin: 26px 0 0;
  max-width: 56ch;
  color: var(--tm-ink-soft);
  font-size: clamp(15px, 1.6vw, 18px);
  line-height: 1.75;
}
.page-prologue.is-center .page-prologue-lead { margin-inline: auto; }

.page-prologue-next { margin-top: 30px; }
.page-prologue-next-link {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding-bottom: 5px;
  color: var(--tm-accent);
  font-family: var(--font-mono);
  font-size: 13px;
  letter-spacing: 0.08em;
  border-bottom: 1px solid var(--tm-line-strong);
  transition: border-color 0.3s ease, gap 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.page-prologue-next-link:hover {
  border-color: var(--tm-accent);
  gap: 14px;
}
</style>
