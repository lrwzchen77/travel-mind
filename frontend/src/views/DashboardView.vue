<script setup>
import { onMounted, onUnmounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import {
  featuredDestinations,
  rotatingCities,
  trustStats,
  marqueeTags,
} from '../layout/menu.js';
import { useReveal } from '../composables/useReveal.js';
import TravelMap3D from '../components/map/AsyncTravelMap3D.vue';
import { cityImageByName } from '../data/cityImages.js';

const root = ref(null);
const cityIndex = ref(0);
const hoverCity = ref(null);
const mapCity = ref('杭州');
let timer;

useReveal(root);

function onMapCity(city) {
  mapCity.value = city;
}

onMounted(() => {
  timer = window.setInterval(() => {
    cityIndex.value = (cityIndex.value + 1) % rotatingCities.length;
  }, 4200);
});

onUnmounted(() => {
  if (timer) window.clearInterval(timer);
});
</script>

<template>
  <div ref="root" class="home-page">
    <section class="home-hero home-hero--luxe">
      <img
        class="home-hero-image"
        :src="cityImageByName[rotatingCities[cityIndex]]"
        :alt="`${rotatingCities[cityIndex]}城市风景`"
      />
      <div class="home-hero-shade" aria-hidden="true" />

      <div class="hero-copy" data-reveal>
        <p class="hero-kicker">
          <span class="pulse-dot" aria-hidden="true" />
          这个周末去哪玩？
        </p>
        <h1>
          下一站，
          <span class="city-swap">
            <em>{{ rotatingCities[cityIndex] }}</em>
          </span>
        </h1>
        <div class="hero-actions">
          <RouterLink class="btn-link btn-coral btn-glow" to="/planning">
            开始规划我的行程
            <span class="btn-arrow" aria-hidden="true">→</span>
          </RouterLink>
          <RouterLink class="btn-link btn-light" to="/trip-history">看看我的行程</RouterLink>
        </div>
      </div>

    </section>

    <div class="marquee" data-reveal aria-label="热门标签">
      <div class="marquee-track">
        <span v-for="(tag, i) in [...marqueeTags, ...marqueeTags]" :key="`${tag}-${i}`" class="marquee-chip">
          {{ tag }}
        </span>
      </div>
    </div>

    <div class="stat-strip" data-reveal>
      <div v-for="item in trustStats" :key="item.label" class="stat-item">
        <strong>
          {{ item.value }}<small v-if="item.suffix">{{ item.suffix }}</small>
        </strong>
        <span>{{ item.label }}</span>
      </div>
    </div>

    <div class="section-head" data-reveal>
      <div>
        <h2>立体地图 · 下一站</h2>
      </div>
      <RouterLink class="text-link" to="/map">
        全屏探索 <span aria-hidden="true">→</span>
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
          <RouterLink class="btn-link btn-coral" :to="{ path: '/planning', query: { city: mapCity } }">
            规划 {{ mapCity }}
          </RouterLink>
          <RouterLink class="btn-link btn-ghost" to="/map">打开立体地图</RouterLink>
        </div>
      </div>
    </div>

    <div class="section-head" data-reveal>
      <div>
        <h2>人气目的地</h2>
      </div>
      <RouterLink class="text-link" to="/cities">
        全部发现 <span aria-hidden="true">→</span>
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
            <span class="season">{{ item.season }}</span>
          </div>
          <strong>{{ item.city }}</strong>
        </div>
        <div class="dest-body">
          <p>{{ item.blurb }}</p>
          <div class="dest-meta">
            <span>{{ item.days }}</span>
            <span class="dest-cta" :class="{ 'is-hot': hoverCity === item.city }">
              先看看 →
            </span>
          </div>
          <div class="dest-hint">{{ item.hint }}</div>
        </div>
      </RouterLink>
    </div>

    <section class="cta-banner" data-reveal>
      <div class="cta-banner-bg" aria-hidden="true" />
      <div class="cta-banner-copy">
        <h2>还在犹豫去哪？</h2>
      </div>
      <div class="actions">
        <RouterLink class="btn-link btn-coral btn-glow" to="/ai-lab">试试 AI 灵感</RouterLink>
        <RouterLink class="btn-link btn-ghost" to="/profile">设置我的偏好</RouterLink>
      </div>
    </section>
  </div>
</template>
