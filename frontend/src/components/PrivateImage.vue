<script setup>
import { onUnmounted, ref, watch } from 'vue';
import { http } from '../api/http.js';

const props = defineProps({
  src: { type: String, default: '' },
  alt: { type: String, default: '' },
  loading: { type: String, default: undefined },
});

const resolved = ref('');
let objectUrl = '';

function revoke() {
  if (objectUrl && globalThis.URL?.revokeObjectURL) URL.revokeObjectURL(objectUrl);
  objectUrl = '';
}

watch(() => props.src, async (value) => {
  revoke();
  resolved.value = '';
  const match = String(value || '').match(/^\/private-uploads\/\d+\/([0-9a-f-]{36}\.(?:jpg|png|webp))$/i);
  if (!match) {
    resolved.value = value || '';
    return;
  }
  try {
    const response = await http.get(`/user/uploads/images/${match[1]}`, { responseType: 'blob' });
    objectUrl = URL.createObjectURL(response.data);
    resolved.value = objectUrl;
  } catch {
    resolved.value = '';
  }
}, { immediate: true });

onUnmounted(revoke);
</script>

<template>
  <img v-if="resolved" :src="resolved" :alt="alt" :loading="loading" />
</template>
