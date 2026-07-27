<script setup>
import { computed, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { Camera, Compass, PenLine } from 'lucide-vue-next';
import { adminAiApi as aiApi } from '../api/ai.js';
import ImageDropUpload from '../components/ImageDropUpload.vue';
import PagePrologue from '../components/PagePrologue.vue';
import { useReveal } from '../composables/useReveal.js';
import { chapterFor } from '../layout/menu.js';

const root = ref(null);
useReveal(root);

// 同一视图服务两个门户：用户端 09 · 助手 / 管理端 A3 · 工具，序号跟随路由章节码
const route = useRoute();
const chapterIndex = chapterFor(route.name).join(' · ');

const loading = ref('');
const error = ref('');
const vision = ref(null);
const trip = ref(null);
const content = ref(null);

const visionForm = reactive({
  image_url: '',
  city: '杭州',
  resource_type: 'attraction',
});
const tripText = ref(JSON.stringify({
  transportation: '公共交通',
  budget: 3000,
  preferences: ['轻松', '美食'],
  days: [
    { date: '2026-08-01', city: '杭州', attractions: ['西湖', '灵隐寺', '河坊街'], weather: '晴' },
  ],
}, null, 2));
const contentForm = reactive({
  text: '西湖风景很好，适合亲子散步，但是节假日排队很久。',
  city: '杭州',
  attraction_name: '西湖',
  language: 'zh',
});

const tripData = computed(() => trip.value?.data || trip.value || {});
const visionData = computed(() => vision.value?.data || vision.value || {});
const contentData = computed(() => content.value?.data || content.value || {});

async function runVision() {
  if (!visionForm.image_url) { error.value = '先上传一张旅行照片'; return; }
  await run('vision', async () => {
    vision.value = await aiApi.detectVision({ ...visionForm });
  });
}

async function runTrip() {
  await run('trip', async () => {
    trip.value = await aiApi.evaluateTrip(JSON.parse(tripText.value));
  });
}

async function runContent() {
  await run('content', async () => {
    content.value = await aiApi.analyzeContent({ ...contentForm });
  });
}

async function run(name, action) {
  loading.value = name;
  error.value = '';
  try {
    await action();
  } catch (err) {
    error.value = err?.message || '这次没跑通，稍后再试';
  } finally {
    loading.value = '';
  }
}
</script>

<template>
  <div ref="root" class="ai-lab-page">
    <PagePrologue
      :index="chapterIndex"
      eyebrow="AI Atelier"
      title="旅行灵感小工具"
      lead="三个轻量实验：看图认地方、估行程累不累、读懂一段游记。把模型的能力拆成可触摸的小动作。"
    />

    <p v-if="error" class="error-line">{{ error }}</p>

    <section class="ai-tools">
      <form class="tool-card field-stack" data-reveal style="--reveal-delay: 0.05s" @submit.prevent="runVision">
        <div class="tool-icon"><Camera :size="24" :stroke-width="2" /></div>
        <h2>认一认这张图</h2>
        <ImageDropUpload v-model="visionForm.image_url" label="拖拽旅行照片到这里，或点击选择" />
        <div class="field-row">
          <div>
            <label class="field-label">城市</label>
            <input v-model="visionForm.city" />
          </div>
          <div>
            <label class="field-label">类型提示</label>
            <input v-model="visionForm.resource_type" placeholder="attraction" />
          </div>
        </div>
        <button type="submit" class="btn-coral" :disabled="loading === 'vision'">
          {{ loading === 'vision' ? '识别中…' : '开始识别' }}
        </button>
        <pre v-if="vision" class="tool-output">{{ JSON.stringify(visionData, null, 2) }}</pre>
      </form>

      <form class="tool-card field-stack" data-reveal style="--reveal-delay: 0.15s" @submit.prevent="runTrip">
        <div class="tool-icon"><Compass :size="24" :stroke-width="2" /></div>
        <h2>这趟会不会太累</h2>
        <div>
          <label class="field-label">行程内容</label>
          <textarea v-model="tripText" class="code-area" rows="10" spellcheck="false" />
        </div>
        <button type="submit" class="btn-coral" :disabled="loading === 'trip'">
          {{ loading === 'trip' ? '评估中…' : '帮我估一下' }}
        </button>
        <pre v-if="trip" class="tool-output">{{ JSON.stringify(tripData, null, 2) }}</pre>
      </form>

      <form class="tool-card field-stack" data-reveal style="--reveal-delay: 0.25s" @submit.prevent="runContent">
        <div class="tool-icon"><PenLine :size="24" :stroke-width="2" /></div>
        <h2>读懂这段游记</h2>
        <div>
          <label class="field-label">游记原文</label>
          <textarea v-model="contentForm.text" rows="5" spellcheck="false" />
        </div>
        <div class="field-row">
          <div>
            <label class="field-label">城市</label>
            <input v-model="contentForm.city" />
          </div>
          <div>
            <label class="field-label">景点</label>
            <input v-model="contentForm.attraction_name" />
          </div>
        </div>
        <button type="submit" class="btn-coral" :disabled="loading === 'content'">
          {{ loading === 'content' ? '分析中…' : '开始分析' }}
        </button>
        <pre v-if="content" class="tool-output">{{ JSON.stringify(contentData, null, 2) }}</pre>
      </form>
    </section>
  </div>
</template>

<style scoped>
.ai-lab-page { max-width: 1240px; margin: 0 auto; padding: 0 0 80px; }

.tool-card {
  position: relative;
  overflow: hidden;
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1),
              border-color 0.3s ease,
              box-shadow 0.4s ease;
}
.tool-card:hover {
  transform: translateY(-2px);
  border-color: var(--tm-accent-soft);
  box-shadow: 0 18px 40px -24px rgba(0, 0, 0, 0.7);
}
.tool-card h2 {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.015em;
  color: var(--tm-ink);
}
.tool-output {
  margin: 0;
  padding: 14px 16px;
  border: 1px solid var(--tm-line);
  border-radius: var(--tm-radius-control);
  background: var(--tm-paper-muted);
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.6;
  color: var(--tm-ink-soft);
  overflow-x: auto;
}
</style>
