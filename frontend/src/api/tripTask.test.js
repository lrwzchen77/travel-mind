import { describe, expect, it, vi } from 'vitest';
import {
  normalizeTripTaskStatus,
  resolveTripWebSocketUrl,
  TripTaskTimeoutError,
  waitForTripTask,
} from './tripTask.js';

class FakeWebSocket {
  static instances = [];

  constructor(url) {
    this.url = url;
    this.readyState = 1;
    FakeWebSocket.instances.push(this);
  }

  emit(payload) {
    this.onmessage?.({ data: JSON.stringify(payload) });
  }

  close() {
    this.readyState = 3;
  }
}

describe('trip task completion', () => {
  it('normalizes the lowercase backend status contract', () => {
    expect(normalizeTripTaskStatus('COMPLETED')).toBe('completed');
    expect(normalizeTripTaskStatus('failed')).toBe('failed');
  });

  it('resolves a relative WebSocket URL against the API server', () => {
    expect(resolveTripWebSocketUrl(
      '/api/trip/ws/task1',
      'http://localhost:8080/api',
      'http://localhost:5173',
    )).toBe('ws://localhost:8080/api/trip/ws/task1');
  });

  it('accepts a lowercase completion event from WebSocket', async () => {
    FakeWebSocket.instances = [];
    const onUpdate = vi.fn();
    const waiting = waitForTripTask({
      taskId: 'task1',
      wsUrl: '/api/trip/ws/task1',
      loadStatus: vi.fn().mockResolvedValue({ status: 'processing', progress: 85 }),
      onUpdate,
      WebSocketImpl: FakeWebSocket,
      pollIntervalMs: 10000,
      maxWaitMs: 10000,
      locationOrigin: 'http://localhost:5173',
      apiBaseUrl: 'http://localhost:8080/api',
    });

    FakeWebSocket.instances[0].emit({ status: 'completed', result: { plan_id: '9001' } });

    await expect(waiting).resolves.toMatchObject({ status: 'completed' });
    expect(onUpdate).toHaveBeenCalled();
  });

  it('uses polling when WebSocket is unavailable', async () => {
    const loadStatus = vi.fn()
      .mockResolvedValueOnce({ status: 'processing', progress: 85 })
      .mockResolvedValueOnce({ status: 'completed', result: { plan_id: '9001' } });

    await expect(waitForTripTask({
      taskId: 'task1',
      loadStatus,
      WebSocketImpl: null,
      pollIntervalMs: 1,
      maxWaitMs: 1000,
    })).resolves.toMatchObject({ status: 'completed' });
  });

  it('reports the last valid state after the background wait limit', async () => {
    const lastState = { status: 'processing', progress: 85 };
    await expect(waitForTripTask({
      taskId: 'task1',
      loadStatus: vi.fn().mockResolvedValue(lastState),
      WebSocketImpl: null,
      pollIntervalMs: 100,
      maxWaitMs: 5,
    })).rejects.toMatchObject({
      name: TripTaskTimeoutError.name,
      lastState,
    });
  });
});
