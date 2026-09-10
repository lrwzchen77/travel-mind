import { http } from './http.js';
import { authSession } from '../auth/session.js';

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');
const isPages = import.meta.env.BASE_URL !== '/';

function unwrap(response) {
  return response.data?.data ?? response.data;
}

export async function readSse(response, onEvent) {
  if (!response.ok || !response.body) throw new Error('暂时无法连接 AI。');
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  while (true) {
    const { done, value } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done });
    const frames = buffer.split(/\r?\n\r?\n/);
    buffer = frames.pop();
    for (const frame of frames) {
      const event = frame.match(/^event:\s*(.+)$/m)?.[1] || 'message';
      const data = frame.match(/^data:\s*(.+)$/m)?.[1];
      if (data) onEvent(event, JSON.parse(data));
    }
    if (done) break;
  }
}

async function mockAskStream(payload, onEvent) {
  const replies = [
    '你好！我是 TravelMind 旅行助手。在演示模式下，我可以为你提供基本的旅行建议。',
    '根据你的问题，我建议你先看看杭州和成都的行程路线，这两个城市非常适合周末短途旅行。',
    '如果你喜欢美食，成都的宽窄巷子和西安的回民街都是不错的选择。',
    '规划行程时，建议先确定城市和天数，再根据偏好选择景点和餐厅。',
  ];
  const reply = replies[Math.floor(Math.random() * replies.length)];
  for (const chunk of reply.match(/.{1,4}/g) || [reply]) {
    await new Promise((r) => setTimeout(r, 50));
    onEvent('token', { token: chunk });
  }
  onEvent('done', { conversation_id: 1 });
}

async function askStream(payload, onEvent) {
  if (isPages) return mockAskStream(payload, onEvent);
  const token = authSession.token();
  const response = await fetch(`${apiBaseUrl}/user/assistant/ask/stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: token } : {}) },
    body: JSON.stringify(payload),
  });
  if (response.status === 401) {
    authSession.clear();
    const error = new Error('登录状态已失效，请重新登录。');
    error.status = 401;
    throw error;
  }
  return readSse(response, onEvent);
}

export function createAssistantApi(client = http) {
  return {
    conversations() { return client.get('/user/assistant/conversations').then(unwrap); },
    conversation(id) { return client.get(`/user/assistant/conversations/${id}`).then(unwrap); },
    rename(id, title) { return client.put(`/user/assistant/conversations/${id}`, { title }).then(unwrap); },
    remove(id) { return client.delete(`/user/assistant/conversations/${id}`).then(unwrap); },
    stop(id) { return client.post(`/user/assistant/conversations/${id}/stop`).then(unwrap); },
    askStream(payload, onEvent) { return askStream(payload, onEvent); },
  };
}

export const assistantApi = createAssistantApi();
