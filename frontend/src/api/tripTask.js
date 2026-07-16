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

export function resolveTripWebSocketUrl(wsUrl, apiBaseUrl, locationOrigin, token = '') {
  if (!wsUrl) return '';
  const origin = locationOrigin || globalThis.location?.origin || 'http://localhost';
  const apiUrl = new URL(apiBaseUrl || '/api', origin);
  const socketUrl = /^wss?:\/\//i.test(wsUrl)
    ? new URL(wsUrl)
    : new URL(wsUrl, `${apiUrl.protocol}//${apiUrl.host}`);
  socketUrl.protocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:';
  if (token) socketUrl.searchParams.set('Authorization', token);
  return socketUrl.toString();
}

function abortError() {
  const error = new Error('任务等待已取消');
  error.name = 'AbortError';
  return error;
}

/**
 * WebSocket delivers progress immediately; polling remains active at a low
 * frequency so proxies or authentication failures cannot lose completion.
 */
export function waitForTripTask({
  taskId,
  wsUrl,
  loadStatus,
  onUpdate = () => {},
  apiBaseUrl = '/api',
  locationOrigin,
  token = '',
  WebSocketImpl = globalThis.WebSocket,
  pollIntervalMs = 2000,
  maxWaitMs = 180000,
  signal,
}) {
  return new Promise((resolve, reject) => {
    let settled = false;
    let lastState = null;
    let pollTimer = 0;
    let timeoutTimer = 0;
    let socket = null;

    const cleanup = () => {
      clearTimeout(pollTimer);
      clearTimeout(timeoutTimer);
      signal?.removeEventListener('abort', onAbort);
      if (socket) {
        socket.onopen = null;
        socket.onmessage = null;
        socket.onerror = null;
        socket.onclose = null;
        if (socket.readyState < 2) socket.close();
      }
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
      if (status === 'failed') {
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
        // A transient polling failure is tolerated while WebSocket is active.
      }
      schedulePoll();
    };

    const onAbort = () => settle(reject, abortError());
    if (signal?.aborted) {
      onAbort();
      return;
    }
    signal?.addEventListener('abort', onAbort, { once: true });

    if (WebSocketImpl && wsUrl) {
      try {
        socket = new WebSocketImpl(resolveTripWebSocketUrl(wsUrl, apiBaseUrl, locationOrigin, token));
        socket.onmessage = (event) => {
          try {
            accept(JSON.parse(event.data));
          } catch {
            // Ignore malformed push data; polling remains authoritative.
          }
        };
      } catch {
        socket = null;
      }
    }

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
