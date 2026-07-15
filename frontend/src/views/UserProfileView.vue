<script setup>
import { onMounted, ref } from 'vue';
import { resourceApi } from '../api/resources.js';

const profile = ref({ user: {}, preference: {} });
const editorText = ref('{\n  "user": {},\n  "preference": {}\n}');
const error = ref('');
const saving = ref(false);
const showAdvanced = ref(false);

async function load() {
  error.value = '';
  try {
    profile.value = await resourceApi.getProfile(1001);
    editorText.value = JSON.stringify(profile.value, null, 2);
  } catch (err) {
    error.value = err?.message || '加载偏好失败';
  }
}

async function save() {
  saving.value = true;
  error.value = '';
  try {
    profile.value = await resourceApi.updateProfile(1001, JSON.parse(editorText.value));
    editorText.value = JSON.stringify(profile.value, null, 2);
  } catch (err) {
    error.value = err?.message || '保存失败，请检查内容格式';
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-intro">
    <p class="eyebrow">旅行偏好</p>
    <h1>你是怎样的旅行者</h1>
    <p>风格和标签会悄悄影响规划结果——越像你，行程越顺手。</p>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <div class="passport">
    <article class="passport-card">
      <div class="label">旅行护照</div>
      <h2>{{ profile.user.nickname || profile.user.username || '旅人' }}</h2>
      <p>{{ profile.user.email || '还没留下邮箱' }}</p>
    </article>
    <article class="passport-card alt">
      <div class="label">出行风格</div>
      <h2>{{ profile.preference.travel_style || '随心所欲' }}</h2>
      <p>{{ profile.preference.preferred_tags || '去设置几个你喜欢的标签吧' }}</p>
    </article>
  </div>

  <section class="glass-panel field-stack" style="max-width: 720px;">
    <h2>更新我的资料</h2>
    <p class="panel-hint">
      当前以结构化内容保存偏好。
      <button type="button" class="btn-ghost btn-sm" style="margin-left: 8px;" @click="showAdvanced = !showAdvanced">
        {{ showAdvanced ? '收起高级编辑' : '打开高级编辑' }}
      </button>
    </p>

    <template v-if="showAdvanced">
      <textarea v-model="editorText" class="code-area" rows="14" spellcheck="false" />
      <div class="actions">
        <button type="button" class="btn-coral" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存偏好' }}
        </button>
        <button type="button" class="btn-ghost" @click="load">重新加载</button>
      </div>
    </template>
    <template v-else>
      <p class="progress-text">
        昵称、邮箱与偏好标签已展示在上方卡片。需要改字段时，点「打开高级编辑」。
      </p>
      <div class="actions">
        <button type="button" class="btn-ghost" @click="load">刷新资料</button>
      </div>
    </template>
  </section>
</template>
