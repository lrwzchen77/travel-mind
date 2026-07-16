import { http } from './http.js';
import { authSession } from '../auth/session.js';

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');

function unwrap(response) {
  return response.data?.data ?? response.data;
}

export async function readSse(response, onEvent) {
  if (!response.ok || !response.body) throw new Error('AI 伴游暂时无法连接。');
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

async function askStream(payload, onEvent) {
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
    askStream(payload, onEvent) { return askStream(payload, onEvent); },
  };
}

export const assistantApi = createAssistantApi();
