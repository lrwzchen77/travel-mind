<script setup>
import { ref } from 'vue';
import { uploadApi } from '../api/upload.js';

const props = defineProps({ modelValue: { type: String, default: '' }, label: { type: String, default: '拖拽图片到这里，或点击选择' } });
const emit = defineEmits(['update:modelValue']);
const input = ref(null);
const dragging = ref(false);
const loading = ref(false);
const error = ref('');

async function upload(file) {
  if (!file) return;
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 8 * 1024 * 1024) {
    error.value = '请选择不超过 8MB 的 JPG、PNG 或 WebP 图片';
    return;
  }
  loading.value = true;
  error.value = '';
  try { emit('update:modelValue', (await uploadApi.image(file)).url); } catch (err) { error.value = err?.message || '图片上传失败'; } finally { loading.value = false; }
}

function choose(event) { upload(event.target.files?.[0]); event.target.value = ''; }
function drop(event) { dragging.value = false; upload(event.dataTransfer.files?.[0]); }
</script>

<template>
  <div class="image-drop" :class="{ 'is-dragging': dragging, 'has-image': modelValue }" @dragover.prevent="dragging = true" @dragleave.prevent="dragging = false" @drop.prevent="drop">
    <input ref="input" type="file" accept="image/jpeg,image/png,image/webp" @change="choose" />
    <button type="button" @click="input?.click()">{{ loading ? '正在上传…' : label }}</button>
    <img v-if="modelValue" :src="modelValue" alt="已上传图片预览" />
    <small>JPG、PNG 或 WebP，最大 8MB</small>
    <p v-if="error" class="error-line">{{ error }}</p>
  </div>
</template>
