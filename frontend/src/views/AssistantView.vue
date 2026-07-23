<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { assistantApi } from '../api/assistant.js';
import { authSession } from '../auth/session.js';
import { markdownBlocks } from '../utils/markdown.js';

const route = useRoute();
const router = useRouter();
const conversations = ref([]);
const activeId = ref(null);
const messages = ref([{ role: 'assistant', content: '还没想清楚怎么安排？告诉我目的地、时间、同行人和预算，我先帮你理顺，再交给行程生成器。' }]);
const text = ref('');
const loading = ref(false);
const error = ref('');
const sourceIds = ref(String(route.query.inspirationIds || '').split(',').map(Number).filter(Boolean).slice(0, 5));
const sourceCount = computed(() => sourceIds.value.length);

async function loadConversations() {
  try { conversations.value = await assistantApi.conversations(); } catch { conversations.value = []; }
}

async function openConversation(id) {
  try { const data = await assistantApi.conversation(id); activeId.value = data.id; messages.value = data.messages || []; } catch (err) { error.value = err?.message || '这段对话没有打开。'; }
}

async function ask(question = text.value) {
  const message = String(question || '').trim();
  if (!message || loading.value) return;
  if (!authSession.isLoggedIn()) {
    router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  loading.value = true;
  error.value = '';
  messages.value.push({ role: 'user', content: message });
  const reply = { role: 'assistant', content: '', streaming: true };
  messages.value.push(reply);
  text.value = '';
  try {
    await assistantApi.askStream({ message, conversation_id: activeId.value, inspiration_ids: sourceIds.value }, (event, data) => {
      if (event === 'delta') reply.content += data.text || '';
      if (event === 'done') { activeId.value = data.conversation_id; reply.sources = data.sources || []; }
    });
    if (!reply.content) throw new Error('AI 没有返回内容。');
    await loadConversations();
  } catch (err) {
    messages.value.splice(-2, 2);
    if (err?.status === 401) {
      router.push({ path: '/login', query: { redirect: route.fullPath } });
      return;
    }
    error.value = err?.message || '这次没有连上 AI。';
  } finally { reply.streaming = false; loading.value = false; }
}

function plan() {
  const context = messages.value
    .filter((item) => item.role === 'user')
    .map((item) => String(item.content || '').trim())
    .filter(Boolean)
    .slice(-6)
    .join('\n')
    .slice(0, 400);
  router.push({
    path: '/map',
    query: { inspirationIds: sourceIds.value.join(','), note: context ? `AI 对话需求：${context}` : '' },
  });
}
function newConversation() { activeId.value = null; messages.value = [{ role: 'assistant', content: '说说这趟旅行想怎么玩？我会先帮你理出目的地、时间、预算和灵感，再交给规划器生成行程。' }]; }
onMounted(loadConversations);
</script>

<template>
  <section class="assistant-page">
    <aside class="assistant-sidebar" aria-label="旅行对话列表">
      <div class="assistant-side-head">
        <div><p class="eyebrow">先问 AI</p><h2>理清复杂旅行要求</h2></div>
        <button type="button" class="btn-ghost" @click="newConversation">新对话</button>
      </div>
      <RouterLink class="assistant-bag-link" to="/inspiration-bag"><span>灵感包</span><strong>{{ sourceCount }} 篇</strong></RouterLink>
      <div v-if="conversations.length" class="assistant-conversation-list">
        <button v-for="item in conversations" :key="item.id" type="button" class="assistant-conversation" :class="{ 'is-active': activeId === item.id }" @click="openConversation(item.id)">{{ item.title }}</button>
      </div>
    </aside>
    <main class="assistant-main">
      <header>
        <div><p class="eyebrow">Travel Mind AI</p><h1>旅行对话</h1></div>
        <button type="button" class="btn-coral" :disabled="!messages.some((item) => item.role === 'user')" @click="plan">生成行程</button>
      </header>
      <div v-if="sourceCount" class="assistant-source-strip"><span>已带入 {{ sourceCount }} 篇社区分享</span><RouterLink to="/inspiration-bag">调整灵感包</RouterLink></div>
      <p v-if="error" class="error-line">{{ error }}</p>
      <div class="assistant-messages" aria-live="polite">
        <article v-for="(item, index) in messages" :key="index" class="assistant-message" :class="`is-${item.role}`"><strong>{{ item.role === 'user' ? '你' : 'Travel Mind AI' }}</strong><template v-if="item.role === 'assistant'"><div class="assistant-markdown"><template v-for="(block, blockIndex) in markdownBlocks(item.content)" :key="blockIndex"><component :is="block.type === 'heading' ? `h${block.level}` : block.type === 'quote' ? 'blockquote' : 'p'" v-if="block.parts" :class="`markdown-${block.type}`"><template v-for="(part, partIndex) in block.parts" :key="partIndex"><strong v-if="part.type === 'strong'">{{ part.text }}</strong><code v-else-if="part.type === 'code'">{{ part.text }}</code><a v-else-if="part.type === 'link'" :href="part.href" target="_blank" rel="noreferrer">{{ part.text }}</a><template v-else>{{ part.text }}</template></template></component><component :is="block.type === 'ordered-list' ? 'ol' : 'ul'" v-else-if="block.items" :class="`markdown-${block.type}`"><li v-for="(parts, itemIndex) in block.items" :key="itemIndex"><template v-for="(part, partIndex) in parts" :key="partIndex"><strong v-if="part.type === 'strong'">{{ part.text }}</strong><code v-else-if="part.type === 'code'">{{ part.text }}</code><a v-else-if="part.type === 'link'" :href="part.href" target="_blank" rel="noreferrer">{{ part.text }}</a><template v-else>{{ part.text }}</template></template></li></component><pre v-else class="markdown-code"><code>{{ block.text }}</code></pre></template><span v-if="item.streaming" class="assistant-cursor" aria-label="正在生成" /></div></template><p v-else>{{ item.content }}</p><div v-if="item.sources?.length" class="chip-row"><span v-for="source in item.sources" :key="source.post_id" class="chip">参考：{{ source.title }}</span></div></article>
      </div>
      <div class="assistant-prompts" aria-label="快捷提问"><button type="button" @click="ask('八月带父母出行三天，预算四千，不想太赶，推荐去哪里？')">带父母，三天别太赶</button><button type="button" @click="ask('我选的这些社区分享有什么冲突？应该怎么取舍？')">检查分享冲突</button><button type="button" @click="ask('按少走路、美食优先的节奏，帮我整理一份规划确认卡。')">整理规划确认卡</button></div>
      <form class="assistant-input" @submit.prevent="ask()">
        <label class="sr-only" for="assistant-question">输入旅行问题</label>
        <textarea id="assistant-question" v-model="text" rows="3" placeholder="例如：想去杭州两天，带父母，预算三千，优先安排灵感包里的早餐和慢游路线…" />
        <button class="btn-coral" type="submit" :disabled="loading">{{ loading ? '思考中…' : '发送' }}</button>
      </form>
    </main>
  </section>
</template>
