export class TripTaskTimeoutError extends Error {
  constructor(lastState) {
    super('行程仍在后台生成，请稍后到「我的行程」查看');
    this.name = 'TripTaskTimeoutError';
    this.lastState = lastState;
  }
}

export function normalizeTripTaskStatus(status) {
  return String(status || '').trim().toLowerCase();
}

function abortError() {
  const error = new Error('任务等待已取消');
  error.name = 'AbortError';
  return error;
}

/**
 * Polling is authoritative and keeps bearer tokens out of WebSocket URLs.
 */
export function waitForTripTask({
  taskId,
  loadStatus,
  onUpdate = () => {},
  pollIntervalMs = 2000,
  maxWaitMs = 180000,
  signal,
}) {
  return new Promise((resolve, reject) => {
    let settled = false;
    let lastState = null;
    let pollTimer = 0;
    let timeoutTimer = 0;

    const cleanup = () => {
      clearTimeout(pollTimer);
      clearTimeout(timeoutTimer);
      signal?.removeEventListener('abort', onAbort);
    };

    const settle = (callback, value) => {
      if (settled) return;
      settled = true;
      cleanup();
      callback(value);
    };

    const accept = (state) => {
      if (!state || settled) return false;
      lastState = state;
      onUpdate(state);
      const status = normalizeTripTaskStatus(state.status);
      if (status === 'completed') {
        settle(resolve, state);
        return true;
      }
      if (status === 'failed' || status === 'cancelled') {
        const error = new Error(state.error || '规划失败');
        error.taskState = state;
        settle(reject, error);
        return true;
      }
      return false;
    };

    const schedulePoll = () => {
      if (!settled) pollTimer = setTimeout(poll, pollIntervalMs);
    };

    const poll = async () => {
      if (settled) return;
      try {
        const state = await loadStatus(taskId);
        if (accept(state)) return;
      } catch {
        // Transient network failures are retried until the overall deadline.
      }
      schedulePoll();
    };

    const onAbort = () => settle(reject, abortError());
    if (signal?.aborted) {
      onAbort();
      return;
    }
    signal?.addEventListener('abort', onAbort, { once: true });

    timeoutTimer = setTimeout(async () => {
      try {
        const state = await loadStatus(taskId);
        if (accept(state)) return;
      } catch {
        // Report the last valid state below.
      }
      settle(reject, new TripTaskTimeoutError(lastState));
    }, maxWaitMs);

    poll();
  });
}
