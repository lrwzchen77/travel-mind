import { describe, expect, it, vi } from 'vitest';
import {
  normalizeTripTaskStatus,
  TripTaskTimeoutError,
  waitForTripTask,
} from './tripTask.js';

describe('trip task completion', () => {
  it('normalizes the lowercase backend status contract', () => {
    expect(normalizeTripTaskStatus('COMPLETED')).toBe('completed');
    expect(normalizeTripTaskStatus('failed')).toBe('failed');
  });

  it('uses polling when WebSocket is unavailable', async () => {
    const loadStatus = vi.fn()
      .mockResolvedValueOnce({ status: 'processing', progress: 85 })
      .mockResolvedValueOnce({ status: 'completed', result: { plan_id: '9001' } });

    await expect(waitForTripTask({
      taskId: 'task1',
      loadStatus,
      pollIntervalMs: 1,
      maxWaitMs: 1000,
    })).resolves.toMatchObject({ status: 'completed' });
  });

  it('reports the last valid state after the background wait limit', async () => {
    const lastState = { status: 'processing', progress: 85 };
    await expect(waitForTripTask({
      taskId: 'task1',
      loadStatus: vi.fn().mockResolvedValue(lastState),
      pollIntervalMs: 100,
      maxWaitMs: 5,
    })).rejects.toMatchObject({
      name: TripTaskTimeoutError.name,
      lastState,
    });
  });
});
