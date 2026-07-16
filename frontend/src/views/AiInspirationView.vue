<script setup>
import { computed, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { aiApi } from '../api/ai.js';

const form = reactive({ city: '杭州', attraction_name: '', text: '' });
const result = ref(null);
const loading = ref(false);
const error = ref('');
const data = computed(() => result.value?.data || result.value || {});
const highlights = computed(() => data.value.highlights || data.value.key_points || data.value.tags || []);
const cautions = computed(() => data.value.cautions || data.value.risks || data.value.negative_points || []);
const planningNote = computed(() => [
  data.value.summary ? `内容摘要：${data.value.summary}` : '',
  highlights.value.length ? `值得安排：${highlights.value.map(String).join('、')}` : '',
  cautions.value.length ? `注意事项：${cautions.value.map(String).join('、')}` : '',
].filter(Boolean).join('；').slice(0, 400));

const samples = [
  {
    label: '西湖夜游',
    city: '杭州',
    attraction_name: '西湖',
    text: '傍晚从断桥走到苏堤，人不多，凉风很舒服。建议带件外套，附近小吃很多，但别在湖边随便买纪念品。',
  },
  {
    label: '成都火锅',
    city: '成都',
    attraction_name: '',
    text: '宽窄巷子人很多，更推荐本地人带去的苍蝇馆子。火锅微辣就够，第二天最好别排太满的行程。',
  },
];

function useSample(sample) {
  form.city = sample.city;
  form.attraction_name = sample.attraction_name;
  form.text = sample.text;
}

async function analyze() {
  loading.value = true;
  error.value = '';
  try {
    result.value = await aiApi.analyzeContent({ ...form, language: 'zh' });
  } catch (err) {
    error.value = err?.message || '这次没有读明白，换一段再试';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="page-intro">
    <p class="eyebrow">AI 灵感</p>
    <h1>把别人的游记，变成你的出发线索</h1>
    <RouterLink class="text-link inspiration-history-link" to="/ai-records">
      查看最近灵感 →
    </RouterLink>
  </section>

  <div class="inspiration-layout">
    <form class="glass-panel inspiration-input" @submit.prevent="analyze">
      <div class="field-row">
        <label>
          <span class="field-label">这座城</span>
          <input v-model="form.city" placeholder="例如：杭州" required />
        </label>
        <label>
          <span class="field-label">景点（可选）</span>
          <input v-model="form.attraction_name" placeholder="例如：西湖" />
        </label>
      </div>

      <label>
        <span class="field-label">游记或朋友评价</span>
        <textarea
          v-model="form.text"
          rows="10"
          placeholder="把看到的游记、评价或朋友推荐粘贴到这里——不用整理，原文就好。"
          required
        />
      </label>

      <div class="inspiration-samples">
        <span class="field-label">没准备好文字？先试一段</span>
        <div class="chip-row">
          <button
            v-for="sample in samples"
            :key="sample.label"
            type="button"
            class="chip-choice"
            @click="useSample(sample)"
          >
            {{ sample.label }}
          </button>
        </div>
      </div>

      <p v-if="error" class="error-line">{{ error }}</p>
      <button type="submit" class="btn-coral" :disabled="loading">
        {{ loading ? '正在读这段内容…' : '提炼旅行线索' }}
      </button>
    </form>

    <section class="inspiration-result" :class="{ 'is-empty': !result }">
      <template v-if="result">
        <p class="eyebrow">给你的线索</p>
        <h2>{{ data.summary || data.sentiment || '这段体验值得参考' }}</h2>

        <div v-if="highlights.length" class="inspiration-block inspiration-block--good">
          <h3>值得期待</h3>
          <ul>
            <li v-for="item in highlights" :key="String(item)">{{ item }}</li>
          </ul>
        </div>

        <div v-if="cautions.length" class="inspiration-block inspiration-block--warn">
          <h3>提前留意</h3>
          <ul>
            <li v-for="item in cautions" :key="String(item)">{{ item }}</li>
          </ul>
        </div>

        <RouterLink
          class="btn-link btn-coral"
          style="margin-top: 8px;"
          :to="{ path: '/planning', query: { city: form.city, note: planningNote } }"
        >
          带着线索去规划 →
        </RouterLink>
      </template>

      <template v-else>
        <div class="inspiration-empty">
          <span class="inspiration-empty-icon" aria-hidden="true">✦</span>
          <strong>旅行线索会出现在这里</strong>
        </div>
      </template>
    </section>
  </div>
</template>
