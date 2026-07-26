<script setup>
import { computed, reactive, ref } from 'vue';
import { ArrowRight, Sparkles } from 'lucide-vue-next';
import { RouterLink } from 'vue-router';
import { aiApi } from '../api/ai.js';
import PagePrologue from '../components/PagePrologue.vue';

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
  <PagePrologue index="09" eyebrow="AI 灵感" title="把别人的游记，变成你的出发线索" lead="贴一段游记链接或文本，AI 会提炼出可执行的旅行线索。" next-label="去问旅行助手" next-to="/assistant" />
  <div class="page-intro-aux">
    <RouterLink class="text-link inspiration-history-link" to="/ai-records">
      查看最近灵感 <ArrowRight :size="15" :stroke-width="2.2" />
    </RouterLink>
  </div>

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
          :to="{ path: '/map', query: { city: form.city, note: planningNote } }"
        >
          带着线索去规划 <ArrowRight :size="15" :stroke-width="2.2" />
        </RouterLink>
      </template>

      <template v-else>
        <div class="inspiration-empty">
          <span class="inspiration-empty-icon" aria-hidden="true"><Sparkles :size="30" :stroke-width="2" /></span>
          <strong>旅行线索会出现在这里</strong>
        </div>
      </template>
    </section>

    <section class="chapter-bridge" data-reveal>
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章 · 09 助手</p>
        <h2 class="chapter-bridge-title">把灵感交给旅行助手</h2>
        <p class="chapter-bridge-lead">AI 给出的线索，下一步是和你的需求对话。打开助手，把灵感折成具体行程。</p>
      </div>
      <RouterLink class="chapter-bridge-cta" to="/assistant">
        <span>去问旅行助手</span>
        <ArrowRight :size="18" :stroke-width="2.2" />
      </RouterLink>
    </section>
  </div>
</template>
