<script setup>
import { onMounted, onUnmounted, ref } from 'vue';
import { useRouter, RouterLink } from 'vue-router';
import {
  featuredDestinations,
  rotatingCities,
  trustStats,
  marqueeTags,
} from '../layout/menu.js';
import { useReveal } from '../composables/useReveal.js';
import TravelMap3D from '../components/map/AsyncTravelMap3D.vue';

const router = useRouter();
const root = ref(null);
const cityIndex = ref(0);
const hoverCity = ref(null);
const mapCity = ref('杭州');
let timer;

useReveal(root);

function planCity(city) {
  router.push({ path: '/planning', query: { city } });
}

function onMapCity(city) {
  mapCity.value = city;
}

onMounted(() => {
  timer = window.setInterval(() => {
    cityIndex.value = (cityIndex.value + 1) % rotatingCities.length;
  }, 2200);
});

onUnmounted(() => {
  if (timer) window.clearInterval(timer);
});
</script>

<template>
  <div ref="root" class="home-page">
    <section class="home-hero home-hero--luxe">
      <div class="hero-mesh" aria-hidden="true" />
      <div class="hero-float f1" aria-hidden="true" />
      <div class="hero-float f2" aria-hidden="true" />
      <div class="hero-float f3" aria-hidden="true" />

      <div class="hero-copy" data-reveal>
        <p class="hero-kicker">
          <span class="pulse-dot" aria-hidden="true" />
          这个周末去哪玩？
        </p>
        <h1>
          下一站，
          <span class="city-swap">
            <Transition name="city-fade" mode="out-in">
              <em :key="rotatingCities[cityIndex]">{{ rotatingCities[cityIndex] }}</em>
            </Transition>
          </span>
        </h1>
        <p class="lead">
          把心血来潮，变成一份说走就走的行程。选好城市和节奏，景点、吃住与预算自动排好——像朋友帮你做攻略。
        </p>
        <div class="hero-actions">
          <RouterLink class="btn-link btn-coral btn-glow" to="/planning">
            开始规划我的行程
            <span class="btn-arrow" aria-hidden="true">→</span>
          </RouterLink>
          <RouterLink class="btn-link btn-light" to="/trip-history">看看我的行程</RouterLink>
        </div>
      </div>

      <div class="hero-ticket" data-reveal aria-hidden="true">
        <div class="ticket">
          <div class="ticket-top">
            <span>BOARDING PASS</span>
            <span>TM · 2026</span>
          </div>
          <div class="ticket-mid">
            <div>
              <small>FROM</small>
              <strong>日常</strong>
            </div>
            <div class="ticket-plane">✈</div>
            <div>
              <small>TO</small>
              <strong>{{ rotatingCities[cityIndex] }}</strong>
            </div>
          </div>
          <div class="ticket-bot">
            <span>智能排程</span>
            <span>可保存 · 可对话</span>
          </div>
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
        <p>真实三维街景视角，点城市飞过去，再决定去哪玩</p>
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
          <p>拖动旋转查看天际线，点顶部城市芯片切换目的地</p>
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
        <p>悬停看细节，点击直接带进规划</p>
      </div>
      <RouterLink class="text-link" to="/cities">
        全部发现 <span aria-hidden="true">→</span>
      </RouterLink>
    </div>

    <div class="dest-grid">
      <button
        v-for="(item, index) in featuredDestinations"
        :key="item.city"
        type="button"
        class="dest-card dest-card--luxe"
        data-reveal
        :style="{ '--reveal-delay': `${index * 80}ms` }"
        @mouseenter="hoverCity = item.city"
        @mouseleave="hoverCity = null"
        @click="planCity(item.city)"
      >
        <div class="dest-cover" :class="`mood-${item.mood}`">
          <span class="dest-shine" aria-hidden="true" />
          <span class="tag">{{ item.tag }}</span>
          <strong>{{ item.city }}</strong>
          <span class="season">{{ item.season }}</span>
        </div>
        <div class="dest-body">
          <p>{{ item.blurb }}</p>
          <div class="dest-meta">
            <span>{{ item.days }}</span>
            <span class="dest-cta" :class="{ 'is-hot': hoverCity === item.city }">
              去规划 →
            </span>
          </div>
          <div class="dest-hint">{{ item.hint }}</div>
        </div>
      </button>
    </div>

    <div class="section-head" data-reveal>
      <div>
        <h2>三步出门</h2>
        <p>从灵感到日程，不用学系统</p>
      </div>
    </div>

    <div class="steps steps--luxe">
      <article
        v-for="(step, i) in [
          { n: '01', t: '说清你想怎么玩', d: '城市、天数、预算、想吃想逛的标签——像聊天一样填就好。' },
          { n: '02', t: '等一份专属日程', d: '自动排程，景点、餐饮、住宿连成一天天的路线故事。' },
          { n: '03', t: '保存、微调、再出发', d: '在「我的行程」回看，也能用 AI 估舒适度或聊聊预算。' },
        ]"
        :key="step.n"
        class="step-card"
        data-reveal
        :style="{ '--reveal-delay': `${i * 100}ms` }"
      >
        <div class="step-num">{{ step.n }}</div>
        <h3>{{ step.t }}</h3>
        <p>{{ step.d }}</p>
        <div class="step-line" aria-hidden="true" />
      </article>
    </div>

    <section class="cta-banner" data-reveal>
      <div class="cta-banner-bg" aria-hidden="true" />
      <div class="cta-banner-copy">
        <h2>还在犹豫去哪？</h2>
        <p>用 AI 认一张图、读一段游记，或估估行程累不累——灵感来了再规划也不迟。</p>
      </div>
      <div class="actions">
        <RouterLink class="btn-link btn-coral btn-glow" to="/ai-lab">试试 AI 灵感</RouterLink>
        <RouterLink class="btn-link btn-ghost" to="/profile">设置我的偏好</RouterLink>
      </div>
    </section>
  </div>
</template>
