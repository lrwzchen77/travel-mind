<script setup>
import { computed, ref } from 'vue';
import { useRouter, useRoute, RouterLink } from 'vue-router';
import TravelMap3D from '../components/map/AsyncTravelMap3D.vue';
import { findDestination, geoDestinations } from '../data/geoDestinations.js';

const router = useRouter();
const route = useRoute();
const mapRef = ref(null);

const selected = ref(String(route.query.city || '杭州'));

const dest = computed(() => findDestination(selected.value));

function onCityChange(city) {
  selected.value = city;
  router.replace({ query: { ...route.query, city } });
}

function planHere() {
  router.push({ path: '/planning', query: { city: selected.value } });
}

function pick(city) {
  selected.value = city;
  mapRef.value?.flyToCity?.(city);
  router.replace({ query: { city } });
}
</script>

<template>
  <div class="explore-map-page">
    <section class="page-intro explore-intro">
      <p class="eyebrow">立体地图</p>
      <h1>在真实三维空间里选下一站</h1>
    </section>

    <div class="map-stage">
      <TravelMap3D
        ref="mapRef"
        :city="selected"
        height="min(68vh, 640px)"
        :auto-orbit="false"
        :show-pois="true"
        @city-change="onCityChange"
      />

      <aside class="map-side-card">
        <span class="map-side-tag">{{ dest.tag }}</span>
        <h2>{{ dest.city }}</h2>
        <p>{{ dest.blurb }}</p>
        <ul v-if="dest.pois?.length" class="map-poi-list">
          <li v-for="poi in dest.pois" :key="poi.name">{{ poi.name }}</li>
        </ul>
        <div class="actions" style="margin-top: 16px;">
          <button type="button" class="btn-coral" @click="planHere">去规划这程</button>
          <RouterLink class="btn-link btn-ghost" to="/cities">发现更多城市</RouterLink>
        </div>
      </aside>
    </div>

    <div class="section-head" style="margin-top: 28px;">
      <div>
        <h2>热门飞航</h2>
      </div>
    </div>

    <div class="dest-grid">
      <button
        v-for="item in geoDestinations.slice(0, 4)"
        :key="item.city"
        type="button"
        class="dest-card dest-card--luxe"
        @click="pick(item.city)"
      >
        <div class="dest-cover" :class="`mood-${item.mood}`">
          <span class="tag">{{ item.tag }}</span>
          <strong>{{ item.city }}</strong>
        </div>
        <div class="dest-body">
          <p>{{ item.blurb }}</p>
          <div class="dest-meta">
            <span>{{ item.pois?.length || 0 }} 个地标</span>
            <span class="dest-cta">飞过去 →</span>
          </div>
        </div>
      </button>
    </div>
  </div>
</template>
