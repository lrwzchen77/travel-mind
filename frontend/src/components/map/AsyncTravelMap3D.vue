<script setup>
/**
 * 异步 + 可见性懒挂载：滚到视口再加载 MapLibre，首页不抢首屏
 */
import { defineAsyncComponent, h, onMounted, onUnmounted, ref } from 'vue';

const props = defineProps({
  /** 提前接近视口时再挂载，避免地图抢占页面首屏资源 */
  whenVisible: { type: Boolean, default: true },
  rootMargin: { type: String, default: '360px' },
});

const shellRef = ref(null);
const visible = ref(!props.whenVisible);
const inner = ref(null);
let observer;

const TravelMap3D = defineAsyncComponent({
  loader: () => import('./TravelMap3D.vue'),
  delay: 40,
  suspensible: false,
  loadingComponent: {
    setup(_, { attrs }) {
      return () =>
        h(
          'div',
          {
            class: 'map3d-placeholder',
            style: { minHeight: attrs.height || '280px', height: attrs.height || '280px' },
          },
          [h('div', { class: 'map-spinner' }), h('p', '立体地图准备中…')],
        );
    },
  },
  errorComponent: {
    setup() {
      return () =>
        h('div', { class: 'map3d-placeholder', style: 'min-height: 200px' }, [
          h('strong', '地图加载失败'),
          h('p', '请刷新页面或检查网络'),
        ]);
    },
  },
});

onMounted(() => {
  if (!props.whenVisible || !shellRef.value) {
    visible.value = true;
    return;
  }
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((e) => e.isIntersecting)) {
        visible.value = true;
        observer?.disconnect();
      }
    },
    { root: null, rootMargin: props.rootMargin, threshold: 0.01 },
  );
  observer.observe(shellRef.value);
});

onUnmounted(() => observer?.disconnect());

defineExpose({
  flyToCity: (...args) => inner.value?.flyToCity?.(...args),
  flyToPoint: (...args) => inner.value?.flyToPoint?.(...args),
  toggleOrbit: (...args) => inner.value?.toggleOrbit?.(...args),
  stopOrbit: (...args) => inner.value?.stopOrbit?.(...args),
});
</script>

<template>
  <div ref="shellRef" class="map-async-shell">
    <TravelMap3D v-if="visible" ref="inner" v-bind="$attrs" />
    <div
      v-else
      class="map3d-placeholder"
      :style="{ height: $attrs.height || '440px', minHeight: '240px' }"
    >
      <div class="map-spinner" />
    </div>
  </div>
</template>
