<script setup>
import { onMounted, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { Contact, Backpack, Utensils, ArrowRight } from 'lucide-vue-next';
import { resourceApi } from '../api/resources.js';
import PagePrologue from '../components/PagePrologue.vue';
import { useReveal } from '../composables/useReveal.js';

const root = ref(null);
useReveal(root);

const form = reactive({
  nickname: '',
  email: '',
  phone: '',
  travel_style: '',
  preferred_city: '',
  preferred_tags: '',
  budget_level: 'medium',
  transportation: '',
  hotel_level: '',
  diet_preference: '',
});

const styleOptions = ['轻松慢游', '美食优先', '文化历史', '亲子友好', '徒步户外', '夜生活'];
const transportOptions = ['公共交通', '高铁+地铁', '自驾', '打车为主', '步行多'];
const hotelOptions = ['经济实惠', '舒适型', '精品民宿', '高星酒店', '随便睡'];
const dietOptions = ['本地菜', '清淡', '爱吃辣', '少油少盐', '素食友好', '海鲜'];

const error = ref('');
const message = ref('');
const saving = ref(false);

function selectedList(field) {
  return String(form[field] || '')
    .split(/[,，、\s]+/)
    .map((s) => s.trim())
    .filter(Boolean);
}

function toggleChip(field, value) {
  const list = selectedList(field);
  const i = list.indexOf(value);
  if (i >= 0) list.splice(i, 1);
  else list.push(value);
  form[field] = list.join('、');
}

function isOn(field, value) {
  return selectedList(field).includes(value);
}

function fill(profile) {
  const user = profile?.user || {};
  const preference = profile?.preference || {};
  Object.keys(form).forEach((key) => {
    if (Object.prototype.hasOwnProperty.call(user, key)) form[key] = user[key] || '';
    if (Object.prototype.hasOwnProperty.call(preference, key)) form[key] = preference[key] || '';
  });
}

async function load() {
  error.value = '';
  try {
    fill(await resourceApi.getProfile());
  } catch (err) {
    error.value = err?.message || '偏好加载失败';
  }
}

async function save() {
  saving.value = true;
  error.value = '';
  message.value = '';
  try {
    const profile = await resourceApi.updateProfile({
      user: {
        nickname: form.nickname,
        email: form.email,
        phone: form.phone,
      },
      preference: {
        travel_style: form.travel_style,
        preferred_city: form.preferred_city,
        preferred_tags: form.preferred_tags,
        budget_level: form.budget_level,
        transportation: form.transportation,
        hotel_level: form.hotel_level,
        diet_preference: form.diet_preference,
      },
    });
    fill(profile);
    message.value = '偏好已保存。下次规划会更贴近你的节奏。';
  } catch (err) {
    error.value = err?.message || '保存失败';
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div ref="root">
  <PagePrologue
    index="08 · 偏好"
    eyebrow="Profile"
    title="让每一程更像你"
    lead="把你的旅行风格、联系方式和偏好城市告诉系统，未来的每一次规划都会更贴近你的节奏。"
  />

  <p v-if="message" class="success-line">{{ message }}</p>
  <p v-if="error" class="error-line">{{ error }}</p>

  <form class="profile-form" @submit.prevent="save">
    <section class="glass-panel profile-card">
      <div class="profile-card-head">
        <span class="profile-emoji" aria-hidden="true"><Contact :size="22" :stroke-width="2" /></span>
        <div>
          <h2>怎么称呼你</h2>
        </div>
      </div>
      <div class="profile-fields">
        <label>
          <span>昵称</span>
          <input v-model="form.nickname" placeholder="路上怎么称呼你" autocomplete="nickname" />
        </label>
        <label>
          <span>邮箱</span>
          <input v-model="form.email" type="email" placeholder="可选" autocomplete="email" />
        </label>
        <label class="span-2">
          <span>手机号</span>
          <input v-model="form.phone" inputmode="tel" placeholder="可选" autocomplete="tel" />
        </label>
      </div>
    </section>

    <section class="glass-panel profile-card">
      <div class="profile-card-head">
        <span class="profile-emoji" aria-hidden="true"><Backpack :size="22" :stroke-width="2" /></span>
        <div>
          <h2>这趟通常怎么玩</h2>
        </div>
      </div>

      <div class="profile-block">
        <span class="field-label">旅行节奏</span>
        <div class="chip-row">
          <button
            v-for="opt in styleOptions"
            :key="opt"
            type="button"
            class="chip-choice"
            :class="{ 'is-on': isOn('travel_style', opt) }"
            @click="toggleChip('travel_style', opt)"
          >
            {{ opt }}
          </button>
        </div>
        <input
          v-model="form.travel_style"
          class="profile-soft-input"
          placeholder="或自己写：例如轻松、文化、美食"
        />
      </div>

      <div class="profile-fields" style="margin-top: 18px;">
        <label>
          <span>常去 / 想去城市</span>
          <input v-model="form.preferred_city" placeholder="例如：杭州、成都" />
        </label>
        <label>
          <span>预算水平</span>
          <select v-model="form.budget_level">
            <option value="economy">经济实惠</option>
            <option value="medium">舒适适中</option>
            <option value="premium">品质优先</option>
          </select>
        </label>
        <label class="span-2">
          <span>偏好标签</span>
          <input v-model="form.preferred_tags" placeholder="湖景、博物馆、夜市、少走路…" />
        </label>
      </div>
    </section>

    <section class="glass-panel profile-card">
      <div class="profile-card-head">
        <span class="profile-emoji" aria-hidden="true"><Utensils :size="22" :stroke-width="2" /></span>
        <div>
          <h2>吃住与出行</h2>
        </div>
      </div>

      <div class="profile-block">
        <span class="field-label">怎么出门</span>
        <div class="chip-row">
          <button
            v-for="opt in transportOptions"
            :key="opt"
            type="button"
            class="chip-choice"
            :class="{ 'is-on': isOn('transportation', opt) }"
            @click="toggleChip('transportation', opt)"
          >
            {{ opt }}
          </button>
        </div>
        <input v-model="form.transportation" class="profile-soft-input" placeholder="或自定义交通偏好" />
      </div>

      <div class="profile-block" style="margin-top: 16px;">
        <span class="field-label">住哪里更舒服</span>
        <div class="chip-row">
          <button
            v-for="opt in hotelOptions"
            :key="opt"
            type="button"
            class="chip-choice"
            :class="{ 'is-on': isOn('hotel_level', opt) }"
            @click="toggleChip('hotel_level', opt)"
          >
            {{ opt }}
          </button>
        </div>
        <input v-model="form.hotel_level" class="profile-soft-input" placeholder="或自定义住宿偏好" />
      </div>

      <div class="profile-block" style="margin-top: 16px;">
        <span class="field-label">饮食口味</span>
        <div class="chip-row">
          <button
            v-for="opt in dietOptions"
            :key="opt"
            type="button"
            class="chip-choice"
            :class="{ 'is-on': isOn('diet_preference', opt) }"
            @click="toggleChip('diet_preference', opt)"
          >
            {{ opt }}
          </button>
        </div>
        <input v-model="form.diet_preference" class="profile-soft-input" placeholder="过敏或不吃的也可以写在这" />
      </div>
    </section>

    <div class="profile-submit-bar">
      <button type="submit" class="btn-coral" :disabled="saving">
        {{ saving ? '保存中…' : '保存我的偏好' }}
      </button>
      <RouterLink class="btn-link btn-ghost" to="/map">保存后去规划 <ArrowRight :size="15" :stroke-width="2.2" /></RouterLink>
    </div>
  </form>

  <section class="chapter-bridge" data-reveal>
    <div class="chapter-bridge-copy">
      <p class="chapter-bridge-eyebrow">下一章 · 02 地图</p>
      <h2 class="chapter-bridge-title">带着偏好，去地图开个头</h2>
      <p class="chapter-bridge-lead">偏好已存好，下一步是落到地图上。打开立体地图，圈出你想去的点，让规划器按你的节奏排程。</p>
    </div>
    <RouterLink class="chapter-bridge-cta" to="/map">
      <span>打开立体地图</span>
      <ArrowRight :size="18" :stroke-width="2.2" />
    </RouterLink>
  </section>
  </div>
</template>
