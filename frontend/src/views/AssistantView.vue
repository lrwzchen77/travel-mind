<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { ArrowRight, Bot, Pencil, Plus, Route as RouteIcon, Send, Sparkles, Square, Trash2, User } from 'lucide-vue-next';
import { assistantApi } from '../api/assistant.js';
import { authSession } from '../auth/session.js';
import { markdownBlocks } from '../utils/markdown.js';
import { useReveal } from '../composables/useReveal.js';
import TravelSprite from '../components/TravelSprite.vue';

const root = ref(null);
useReveal(root);

const route = useRoute();
const router = useRouter();
const conversations = ref([]);
const activeId = ref(null);
const messages = ref([{ role: 'assistant', content: '还没想清楚怎么安排？告诉我目的地、时间、同行人和预算，我先帮你理顺，再交给行程生成器。' }]);
const text = ref('');
const loading = ref(false);
const error = ref('');
const messagesEl = ref(null);
const sourceIds = ref(String(route.query.inspirationIds || '').split(',').map(Number).filter(Boolean).slice(0, 5));
const sourceCount = computed(() => sourceIds.value.length);
const hasUserMessage = computed(() => messages.value.some((item) => item.role === 'user'));

// 快捷提问：图标 + 文案，作为对话的"起手式"
const quickPrompts = [
  { icon: User, label: '带父母，三天别太赶', ask: '八月带父母出行三天，预算四千，不想太赶，推荐去哪里？' },
  { icon: Sparkles, label: '检查分享冲突', ask: '我选的这些社区分享有什么冲突？应该怎么取舍？' },
  { icon: RouteIcon, label: '整理规划确认卡', ask: '按少走路、美食优先的节奏，帮我整理一份规划确认卡。' },
];

// 流式生成时跟随滚动到最新内容
watch(messages, async () => {
  await nextTick();
  const el = messagesEl.value;
  if (el) el.scrollTop = el.scrollHeight;
}, { deep: true });

async function loadConversations() {
  try { conversations.value = await assistantApi.conversations(); } catch { conversations.value = []; }
}

async function openConversation(id) {
  try { const data = await assistantApi.conversation(id); activeId.value = data.id; messages.value = data.messages || []; } catch (err) { error.value = err?.message || '这段对话没有打开。'; }
}

async function renameConversation(item) {
  const title = window.prompt('新的对话标题', item.title);
  if (!title?.trim()) return;
  try { await assistantApi.rename(item.id, title); await loadConversations(); } catch (err) { error.value = err?.message || '重命名失败。'; }
}

async function deleteConversation(item) {
  if (!window.confirm(`删除“${item.title}”？`)) return;
  try {
    await assistantApi.remove(item.id);
    if (activeId.value === item.id) newConversation();
    await loadConversations();
  } catch (err) { error.value = err?.message || '删除失败。'; }
}

async function stopGeneration() {
  if (!activeId.value) return;
  try { await assistantApi.stop(activeId.value); } catch (err) { error.value = err?.message || '停止失败。'; }
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
      if (event === 'start') activeId.value = data.conversation_id;
      if (event === 'delta') reply.content += data.text || '';
      if (event === 'done') {
        activeId.value = data.conversation_id;
        reply.sources = data.sources || [];
        reply.mode = data.mode;
        reply.model = data.model;
        if (data.mode === 'stopped' && !reply.content) reply.content = '已停止生成。';
      }
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
  <section ref="root" class="assistant-page">
    <aside class="assistant-sidebar" aria-label="旅行对话列表" data-reveal>
      <div class="assistant-side-head">
        <div>
          <p class="eyebrow"><span class="type-index">S.09</span> 先问 AI</p>
          <h2>理清复杂旅行要求</h2>
        </div>
        <button type="button" class="assistant-new" @click="newConversation">
          <Plus :size="14" :stroke-width="2.4" />
          新对话
        </button>
      </div>
      <RouterLink class="assistant-bag-link" to="/inspiration-bag">
        <span class="assistant-bag-label"><Sparkles :size="14" :stroke-width="2" /> 灵感包</span>
        <strong>{{ sourceCount }} 篇</strong>
      </RouterLink>
      <p class="assistant-log-label">DIALOGUE LOG · {{ String(conversations.length).padStart(2, '0') }}</p>
      <div v-if="conversations.length" class="assistant-conversation-list">
        <div
          v-for="item in conversations"
          :key="item.id"
          class="assistant-conversation-row"
          :class="{ 'is-active': activeId === item.id }"
        >
          <button type="button" class="assistant-conversation" :class="{ 'is-active': activeId === item.id }" @click="openConversation(item.id)"><span>{{ item.title }}</span></button>
          <button type="button" class="assistant-icon-btn" aria-label="重命名对话" @click="renameConversation(item)"><Pencil :size="13" :stroke-width="2" /></button>
          <button type="button" class="assistant-icon-btn is-danger" aria-label="删除对话" @click="deleteConversation(item)"><Trash2 :size="13" :stroke-width="2" /></button>
        </div>
      </div>
      <p v-else class="assistant-log-empty">还没有对话档案。<br />从右侧提出第一个问题开始。</p>
    </aside>

    <main class="assistant-main">
      <!-- 巨型轮廓字：对话区的空间锚点 -->
      <span class="assistant-outline type-outline" aria-hidden="true">DIALOGUE</span>

      <header data-reveal>
        <div>
          <p class="eyebrow">
            Travel Mind AI
            <span class="assistant-live" :class="{ 'is-busy': loading }" aria-hidden="true">
              <i />{{ loading ? 'GENERATING' : 'STANDBY' }}
            </span>
          </p>
          <h1>旅行对话</h1>
        </div>
        <button type="button" class="btn-coral btn-fluid" data-magnetic :disabled="!hasUserMessage" @click="plan">
          <RouteIcon :size="15" :stroke-width="2.2" />
          生成行程
        </button>
      </header>

      <div v-if="sourceCount" class="assistant-source-strip" data-reveal>
        <span>已带入 {{ sourceCount }} 篇社区分享</span>
        <RouterLink to="/inspiration-bag">调整灵感包</RouterLink>
      </div>
      <p v-if="error" class="error-line">{{ error }}</p>

      <div ref="messagesEl" class="assistant-messages" :class="{ 'is-empty': !hasUserMessage }" aria-live="polite">
        <!-- 空态舞台：旅途精灵坐镇对话区，生成时进入思考态 -->
        <div v-if="!hasUserMessage" class="assistant-sprite-stage" aria-hidden="true">
          <TravelSprite :state="loading ? 'thinking' : 'idle'" :size="148" />
          <p class="assistant-sprite-name">TM-09 · 旅途精灵</p>
          <p class="assistant-sprite-tag">DIGITAL TRAVEL COMPANION</p>
        </div>
        <article v-for="(item, index) in messages" :key="index" class="assistant-message" :class="`is-${item.role}`" :data-reveal="index < 3 ? '' : null">
          <span class="assistant-role">
            <component :is="item.role === 'user' ? User : Bot" :size="12" :stroke-width="2.2" aria-hidden="true" />
            <strong>{{ item.role === 'user' ? '你' : 'Travel Mind AI' }}</strong>
            <small v-if="item.role === 'assistant' && item.mode">{{ item.mode === 'fallback' ? '本地降级回复' : item.mode === 'stopped' ? '已停止' : item.model }}</small>
          </span>
          <template v-if="item.role === 'assistant'"><div class="assistant-markdown"><template v-for="(block, blockIndex) in markdownBlocks(item.content)" :key="blockIndex"><component :is="block.type === 'heading' ? `h${block.level}` : block.type === 'quote' ? 'blockquote' : 'p'" v-if="block.parts" :class="`markdown-${block.type}`"><template v-for="(part, partIndex) in block.parts" :key="partIndex"><strong v-if="part.type === 'strong'">{{ part.text }}</strong><code v-else-if="part.type === 'code'">{{ part.text }}</code><a v-else-if="part.type === 'link'" :href="part.href" target="_blank" rel="noreferrer">{{ part.text }}</a><template v-else>{{ part.text }}</template></template></component><component :is="block.type === 'ordered-list' ? 'ol' : 'ul'" v-else-if="block.items" :class="`markdown-${block.type}`"><li v-for="(parts, itemIndex) in block.items" :key="itemIndex"><template v-for="(part, partIndex) in parts" :key="partIndex"><strong v-if="part.type === 'strong'">{{ part.text }}</strong><code v-else-if="part.type === 'code'">{{ part.text }}</code><a v-else-if="part.type === 'link'" :href="part.href" target="_blank" rel="noreferrer">{{ part.text }}</a><template v-else>{{ part.text }}</template></template></li></component><pre v-else class="markdown-code"><code>{{ block.text }}</code></pre></template><span v-if="item.streaming" class="assistant-cursor" aria-label="正在生成" /></div></template>
          <p v-else>{{ item.content }}</p>
          <div v-if="item.sources?.length" class="chip-row"><span v-for="source in item.sources" :key="source.post_id" class="chip">参考：{{ source.title }}</span></div>
        </article>
      </div>

      <div class="assistant-prompts" aria-label="快捷提问" data-reveal>
        <button v-for="prompt in quickPrompts" :key="prompt.label" type="button" @click="ask(prompt.ask)">
          <component :is="prompt.icon" :size="13" :stroke-width="2" aria-hidden="true" />
          {{ prompt.label }}
        </button>
        <!-- 对话展开后，精灵缩小停靠在快捷提问右侧继续值班 -->
        <span v-if="hasUserMessage" class="assistant-sprite-dock" aria-hidden="true">
          <TravelSprite :state="loading ? 'thinking' : 'idle'" :size="46" />
        </span>
      </div>

      <form class="assistant-input" data-reveal @submit.prevent="ask()">
        <label class="sr-only" for="assistant-question">输入旅行问题</label>
        <textarea
          id="assistant-question"
          v-model="text"
          rows="2"
          placeholder="例如：想去杭州两天，带父母，预算三千，优先安排灵感包里的早餐和慢游路线…"
          @keydown.enter.exact.prevent="ask()"
        />
        <div class="assistant-input-side">
          <button v-if="loading" class="assistant-stop" type="button" :disabled="!activeId" aria-label="停止生成" @click="stopGeneration">
            <Square :size="13" :stroke-width="2.4" />
          </button>
          <button class="assistant-send" type="submit" :disabled="loading" data-magnetic aria-label="发送">
            <Send :size="17" :stroke-width="2.2" />
          </button>
        </div>
        <span class="assistant-input-hint" aria-hidden="true">ENTER 发送 · SHIFT+ENTER 换行</span>
      </form>
    </main>

    <section class="chapter-bridge" data-reveal>
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章</p>
        <h2 class="chapter-bridge-title">把对话折成行程</h2>
        <p class="chapter-bridge-lead">理清需求后，下一步是落到地图上。打开规划器，把刚才聊到的城市、节奏、预算转成可走的路线。</p>
      </div>
      <RouterLink class="chapter-bridge-cta" to="/map">
        <span>去地图规划</span>
        <ArrowRight :size="18" :stroke-width="2.2" />
      </RouterLink>
    </section>
  </section>
</template>

<style scoped>
.assistant-message {
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.4s ease;
}
.assistant-message.is-inview {
  animation: assistant-msg-in 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
@keyframes assistant-msg-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
/* 桥接卡横跨整行，收在工作台下方 */
.assistant-page .chapter-bridge { grid-column: 1 / -1; margin-top: 8px; }
</style>
