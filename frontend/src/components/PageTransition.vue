<script setup>
import { RouterView } from 'vue-router';
</script>

<template>
  <RouterView v-slot="{ Component, route }">
    <Transition name="page" mode="out-in">
      <div :key="route.path" class="page-transition-shell">
        <component :is="Component" />
      </div>
    </Transition>
  </RouterView>
</template>
