<script setup>
import { onMounted, onUnmounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ArrowRight, ArrowUpRight, MapPin, Clock, Compass, Map as MapIcon, Sparkles } from 'lucide-vue-next';
import {
  featuredDestinations,
  rotatingCities,
  marqueeTags,
} from '../layout/menu.js';
import { useReveal } from '../composables/useReveal.js';
import TravelMap3D from '../components/map/AsyncTravelMap3D.vue';
import { cityImageByName } from '../data/cityImages.js';
import { supportsPlanning } from '../data/planningSupport.js';

const root = ref(null);
const cityIndex = ref(0);
const hoverCity = ref(null);
const mapCity = ref('杭州');
const scrollY = ref(0);
let timer;

useReveal(root);

function onMapCity(city) {
  mapCity.value = city;
}

function onScroll() {
  scrollY.value = window.scrollY;
}

onMounted(() => {
  timer = window.setInterval(() => {
    cityIndex.value = (cityIndex.value + 1) % rotatingCities.length;
  }, 4200);
  window.addEventListener('scroll', onScroll, { passive: true });
});

onUnmounted(() => {
  if (timer) window.clearInterval(timer);
  window.removeEventListener('scroll', onScroll);
});
</script>

<template>
  <div ref="root" class="home-page">
    <!-- Cinematic hero -->
    <section class="home-hero home-hero--luxe">
      <img
        v-for="(city, i) in rotatingCities"
        :key="city"
        class="home-hero-image"
        :class="{ 'is-active': i === cityIndex }"
        :style="{ opacity: i === cityIndex ? 1 : 0, transform: `scale(${1.06 + scrollY * 0.0004})` }"
        :src="cityImageByName[city]"
        :alt="`${city}城市风景`"
      />
      <div class="home-hero-shade" aria-hidden="true" />

      <div class="hero-copy" data-reveal>
        <p class="hero-kicker">
          <span class="pulse-dot" aria-hidden="true" />
          这个周末去哪玩？
        </p>
        <h1>
          <span class="hero-line">下一站，</span>
          <span class="city-swap" :key="cityIndex">
            <em>{{ rotatingCities[cityIndex] }}</em>
          </span>
        </h1>
        <p class="hero-sub">把一条灵感，折叠成一份走得通的行程。</p>
        <div class="hero-actions">
          <RouterLink class="btn-link btn-coral btn-glow" :to="{ path: '/map', query: { city: rotatingCities[cityIndex] } }">
            开始规划我的行程
            <span class="btn-arrow" aria-hidden="true"><ArrowRight :size="16" :stroke-width="2.4" /></span>
          </RouterLink>
          <RouterLink class="btn-link btn-light" to="/trip-history">看看我的行程</RouterLink>
        </div>
      </div>

      <div class="hero-side" data-reveal aria-hidden="true">
        <span class="hero-side-label">NOW BOARDING</span>
        <span class="hero-side-city">{{ rotatingCities[cityIndex] }}</span>
        <span class="hero-side-meta">TM · 2026 · 智能排程</span>
      </div>
    </section>

    <!-- Kinetic marquee -->
    <div class="marquee" data-reveal aria-label="热门标签">
      <div class="marquee-track">
        <span v-for="(tag, i) in [...marqueeTags, ...marqueeTags]" :key="`${tag}-${i}`" class="marquee-chip">
          {{ tag }}
        </span>
      </div>
    </div>

    <!-- Destinations -->
    <div class="section-head" data-reveal>
      <div>
        <p class="eyebrow">先选一座城</p>
        <h2>人气目的地</h2>
      </div>
      <RouterLink class="text-link" to="/cities">
        全部发现 <span aria-hidden="true"><ArrowRight :size="15" :stroke-width="2.2" /></span>
      </RouterLink>
    </div>

    <div class="dest-grid">
      <RouterLink
        v-for="(item, index) in featuredDestinations"
        :key="item.city"
        class="dest-card dest-card--luxe"
        :to="`/city/${encodeURIComponent(item.city)}`"
        data-reveal
        :style="{ '--reveal-delay': `${index * 80}ms` }"
        @mouseenter="hoverCity = item.city"
        @mouseleave="hoverCity = null"
      >
        <div class="dest-cover" :class="`mood-${item.mood}`">
          <img class="dest-cover-image" :src="cityImageByName[item.city]" :alt="`${item.city}城市风景`" />
          <span class="dest-shine" aria-hidden="true" />
          <div class="dest-labels">
            <span class="tag">{{ item.tag }}</span>
          </div>
          <strong>{{ item.city }}</strong>
          <span class="dest-arrow" aria-hidden="true"><ArrowUpRight :size="22" :stroke-width="2" /></span>
        </div>
        <div class="dest-body">
          <p>{{ item.blurb }}</p>
          <div class="dest-meta">
            <span class="dest-meta-days"><Clock :size="13" :stroke-width="2" /> {{ item.days }}</span>
            <span class="dest-cta" :class="{ 'is-hot': hoverCity === item.city }">
              先看看 <ArrowRight :size="14" :stroke-width="2.2" />
            </span>
          </div>
          <div class="dest-hint"><MapPin :size="12" :stroke-width="2" /> {{ item.hint }}</div>
        </div>
      </RouterLink>
    </div>

    <!-- Map -->
    <div class="section-head" data-reveal>
      <div>
        <p class="eyebrow">再看路线距离</p>
        <h2>立体地图 · 下一站</h2>
      </div>
      <RouterLink class="text-link" to="/map">
        全屏探索 <span aria-hidden="true"><ArrowRight :size="15" :stroke-width="2.2" /></span>
      </RouterLink>
    </div>

    <div class="home-map-wrap" data-reveal>
      <TravelMap3D
        when-visible
        :city="mapCity"
        height="440px"
        compact
        :auto-orbit="false"
        @city-change="onMapCity"
      />
      <div class="home-map-cta">
        <div>
          <strong>当前镜头：{{ mapCity }}</strong>
        </div>
        <div class="actions">
          <RouterLink v-if="supportsPlanning(mapCity)" class="btn-link btn-coral" :to="{ path: '/map', query: { city: mapCity } }">
            <Compass :size="15" :stroke-width="2.2" />
            规划 {{ mapCity }}
          </RouterLink>
          <RouterLink v-else class="btn-link btn-coral" :to="`/city/${encodeURIComponent(mapCity)}`">先浏览 {{ mapCity }}</RouterLink>
          <RouterLink class="btn-link btn-ghost" to="/map">
            <MapIcon :size="15" :stroke-width="2" />
            打开立体地图
          </RouterLink>
        </div>
      </div>
    </div>

    <!-- Closing manifesto -->
    <section class="home-manifesto" data-reveal>
      <Sparkles :size="18" :stroke-width="2" class="home-manifesto-mark" />
      <p>
        从一条灵感，到一份<span>走得通</span>的行程。<br />
        Travel Mind 把城市情报、地图测距与 AI 排程折叠进同一次对话。
      </p>
    </section>

    <!-- Chapter bridge: 00 · 序 → 01 · 规划 -->
    <section class="chapter-bridge" data-reveal>
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章 · 01 规划</p>
        <h2 class="chapter-bridge-title">从一句话，开出一份行程</h2>
        <p class="chapter-bridge-lead">城市、节奏、预算、灵感——告诉规划器你的约束，它会在地图上折出一条走得通的路线。</p>
      </div>
      <RouterLink class="chapter-bridge-cta" to="/planning">
        <span>开始规划</span>
        <ArrowRight :size="18" :stroke-width="2.2" />
      </RouterLink>
    </section>

  </div>
</template>

<style scoped>
.hero-line { display: block; color: rgba(255, 255, 255, 0.78); }
.hero-sub {
  margin: 22px 0 0;
  max-width: 460px;
  color: rgba(255, 255, 255, 0.7);
  font-size: clamp(15px, 1.6vw, 18px);
  line-height: 1.6;
}
.hero-side {
  position: absolute;
  right: clamp(24px, 5vw, 56px);
  bottom: clamp(40px, 7vw, 84px);
  z-index: 2;
  display: none;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  text-align: right;
  writing-mode: vertical-rl;
  transform: rotate(180deg);
}
@media (min-width: 980px) { .hero-side { display: flex; } }
.hero-side-label {
  color: var(--tm-accent);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.3em;
  text-transform: uppercase;
}
.hero-side-city {
  font-family: var(--font-display);
  font-size: clamp(40px, 5vw, 64px);
  font-weight: 700;
  color: #fff;
  letter-spacing: -0.02em;
  line-height: 1;
}
.hero-side-meta {
  color: rgba(255, 255, 255, 0.5);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

.dest-arrow {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 2;
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(12, 10, 8, 0.5);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: #fff;
  opacity: 0;
  transform: translate(-6px, 6px) rotate(-20deg);
  transition: opacity 0.4s ease, transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.dest-card:hover .dest-arrow { opacity: 1; transform: translate(0, 0) rotate(0); }

.dest-meta-days { display: inline-flex; align-items: center; gap: 5px; }
.dest-cta { display: inline-flex; align-items: center; gap: 5px; }
.dest-hint { display: flex; align-items: center; gap: 7px; }

.home-manifesto {
  margin: 80px 0 0;
  padding: 56px clamp(28px, 5vw, 64px);
  border: 1px solid var(--tm-line);
  border-radius: var(--tm-radius-lg);
  background:
    radial-gradient(120% 100% at 80% 0%, var(--tm-accent-soft), transparent 60%),
    var(--tm-paper);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}
.home-manifesto-mark { color: var(--tm-accent); }
.home-manifesto p {
  margin: 0;
  max-width: 620px;
  font-family: var(--font-display);
  font-size: clamp(20px, 2.6vw, 30px);
  font-weight: 600;
  line-height: 1.45;
  letter-spacing: -0.01em;
  color: var(--tm-ink);
}
.home-manifesto span { color: var(--tm-accent); }
</style>
