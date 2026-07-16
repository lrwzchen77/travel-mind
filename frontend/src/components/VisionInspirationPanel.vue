<script setup>
import { computed, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { aiApi } from '../api/ai.js';
import { authSession } from '../auth/session.js';
import {
  buildVisionPlanningQuery,
  formatVisionConfidence,
  visionLabelMeta,
} from '../data/visionInsights.js';

const route = useRoute();
const router = useRouter();
const city = ref(String(route.query.city || ''));
const previewUrl = ref('');
const result = ref(null);
const loading = ref(false);
const error = ref('');
const loggedIn = authSession.isLoggedIn();

const resultData = computed(() => result.value?.data || result.value || {});
const prediction = computed(() => resultData.value.labels?.[0] || null);
const predictionMeta = computed(() => visionLabelMeta(prediction.value?.name));
const trained = computed(() => resultData.value.model_mode === 'trained_yolo');
const confidence = computed(() => formatVisionConfidence(prediction.value?.confidence));
const risks = computed(() => resultData.value.risk_hints || []);

function readAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(new Error('照片读取失败'));
    reader.readAsDataURL(file);
  });
}

async function selectImage(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  error.value = '';
  result.value = null;
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    error.value = '请选择 JPG、PNG 或 WebP 图片';
    return;
  }
  if (file.size > 8 * 1024 * 1024) {
    error.value = '图片不能超过 8MB';
    return;
  }
  loading.value = true;
  try {
    previewUrl.value = await readAsDataUrl(file);
    result.value = await aiApi.detectVision({
      image_url: previewUrl.value,
      city: city.value.trim() || '待选城市',
      resource_type: 'travel_scene',
    });
  } catch (err) {
    error.value = err?.message || '这张照片暂时没认出来，请换一张试试';
  } finally {
    loading.value = false;
    event.target.value = '';
  }
}

function goPlanning() {
  if (!city.value.trim()) {
    error.value = '先填一座想去的城市，再带进规划';
    return;
  }
  router.push({
    path: '/planning',
    query: buildVisionPlanningQuery({
      city: city.value,
      prediction: prediction.value,
      trained: trained.value,
    }),
  });
}
</script>

<template>
  <section class="vision-inspiration" aria-labelledby="vision-inspiration-title">
    <div class="vision-inspiration-copy">
      <p class="eyebrow">本地 AI 看图</p>
      <h2 id="vision-inspiration-title">从一张照片，找到这趟旅行的感觉</h2>
      <label v-if="loggedIn" class="vision-upload">
        <input type="file" accept="image/jpeg,image/png,image/webp" @change="selectImage" />
        <span>{{ loading ? '正在识别…' : previewUrl ? '换一张照片' : '上传照片' }}</span>
      </label>
      <RouterLink
        v-else
        class="btn-link btn-coral"
        :to="{ path: '/login', query: { redirect: '/cities?vision=1' } }"
      >登录后看图</RouterLink>
      <p v-if="error" class="error-line vision-error">{{ error }}</p>
    </div>

    <div class="vision-result" :class="{ 'has-image': previewUrl }">
      <div v-if="previewUrl" class="vision-preview">
        <img :src="previewUrl" alt="等待分析的旅行照片" />
      </div>
      <div v-if="result" class="vision-result-copy">
        <span class="vision-source" :class="{ 'is-local': trained }">
          {{ trained ? '本地训练模型' : '基础场景判断' }}
        </span>
        <h3>{{ predictionMeta.label }}</h3>
        <p v-if="confidence">判断置信度 {{ confidence }}</p>
        <p v-for="risk in risks" :key="risk" class="vision-risk">{{ risk }}</p>
        <label class="vision-city-field">
          <span>准备去哪座城</span>
          <input v-model="city" placeholder="例如：重庆" />
        </label>
        <button type="button" class="btn-coral" @click="goPlanning">按这个感觉规划</button>
      </div>
      <div v-else-if="loading" class="vision-placeholder" aria-live="polite">
        <span class="vision-scan-line" aria-hidden="true" />
        <strong>本地模型正在看图</strong>
      </div>
      <div v-else class="vision-placeholder">
        <strong>选择一张旅行照片</strong>
      </div>
    </div>
  </section>
</template>
