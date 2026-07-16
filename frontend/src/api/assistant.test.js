import { describe, expect, it, vi } from 'vitest';
import { createAssistantApi, readSse } from './assistant.js';
import { authSession } from '../auth/session.js';

describe('assistant API', () => {
  it('emits SSE events split across network chunks', async () => {
    const encoder = new TextEncoder();
    const body = new ReadableStream({
      start(controller) {
        controller.enqueue(encoder.encode('event: delta\ndata: {"text":"旅行"}\n\n'));
        controller.enqueue(encoder.encode('event: done\ndata: {"conversation_id":7}\n\n'));
        controller.close();
      },
    });
    const events = [];
    await readSse({ ok: true, body }, (event, data) => events.push([event, data]));
    expect(events).toEqual([['delta', { text: '旅行' }], ['done', { conversation_id: 7 }]]);
  });

  it('clears an expired session before reporting a streaming 401', async () => {
    authSession.save({ tokenValue: 'expired', user: { roles: ['user'] } });
    const originalFetch = globalThis.fetch;
    globalThis.fetch = vi.fn().mockResolvedValue({ status: 401 });
    await expect(createAssistantApi().askStream({ message: '测试' }, () => {})).rejects.toMatchObject({ status: 401 });
    expect(authSession.isLoggedIn()).toBe(false);
    globalThis.fetch = originalFetch;
  });
});
