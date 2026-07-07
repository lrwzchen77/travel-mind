<script setup>
import { onMounted, ref } from 'vue';
import { resourceApi } from '../api/resources.js';

const profile = ref({ user: {}, preference: {} });
const editorText = ref('{\n  "user": {},\n  "preference": {}\n}');
const error = ref('');

async function load() {
  error.value = '';
  try {
    profile.value = await resourceApi.getProfile(1001);
    editorText.value = JSON.stringify(profile.value, null, 2);
  } catch (err) {
    error.value = err?.message || 'Request failed';
  }
}

async function save() {
  profile.value = await resourceApi.updateProfile(1001, JSON.parse(editorText.value));
  editorText.value = JSON.stringify(profile.value, null, 2);
}

onMounted(load);
</script>

<template>
  <section class="page-header">
    <h1>User Profile</h1>
  </section>

  <p v-if="error" class="error-line">{{ error }}</p>

  <section class="crud-layout">
    <div class="status-grid">
      <article class="status-card">
        <h2>{{ profile.user.nickname || profile.user.username || 'User' }}</h2>
        <p>{{ profile.user.email || 'No email' }}</p>
      </article>
      <article class="status-card">
        <h2>{{ profile.preference.travel_style || 'Travel Style' }}</h2>
        <p>{{ profile.preference.preferred_tags || 'No tags' }}</p>
      </article>
    </div>

    <form class="editor-panel" @submit.prevent="save">
      <h2>Profile JSON</h2>
      <textarea v-model="editorText" rows="18" spellcheck="false"></textarea>
      <div class="actions">
        <button type="submit">Save</button>
        <button type="button" @click="load">Reload</button>
      </div>
    </form>
  </section>
</template>
