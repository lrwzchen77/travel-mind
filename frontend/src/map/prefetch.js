/**
 * 地图加载加速：预连接 CDN、缓存 style JSON、预热 MapLibre 模块
 */

const STYLE_URL =
  import.meta.env.VITE_MAP_STYLE_URL || 'https://tiles.openfreemap.org/styles/liberty';

const TILE_ORIGIN = 'https://tiles.openfreemap.org';

/** @type {Promise<object>|null} */
let stylePromise = null;
/** @type {Promise<typeof import('maplibre-gl')>|null} */
let maplibrePromise = null;

export function getStyleUrl() {
  return STYLE_URL;
}

/** 尽早 DNS / TLS 握手 */
export function preconnectMapCdn() {
  if (typeof document === 'undefined') return;
  const ensure = (rel, href, crossOrigin = true) => {
    if (document.querySelector(`link[rel="${rel}"][href="${href}"]`)) return;
    const link = document.createElement('link');
    link.rel = rel;
    link.href = href;
    if (crossOrigin) link.crossOrigin = 'anonymous';
    document.head.appendChild(link);
  };
  ensure('preconnect', TILE_ORIGIN);
  ensure('dns-prefetch', TILE_ORIGIN, false);
}

/**
 * 拉取并缓存 style（后续 Map 实例直接用对象，少一次串行等待）
 */
export function prefetchMapStyle() {
  if (stylePromise) return stylePromise;
  preconnectMapCdn();
  stylePromise = fetch(STYLE_URL, {
    mode: 'cors',
    credentials: 'omit',
    // 浏览器 HTTP 缓存友好
    cache: 'force-cache',
  })
    .then((res) => {
      if (!res.ok) throw new Error(`style ${res.status}`);
      return res.json();
    })
    .catch((err) => {
      // 失败时回退 URL，让 MapLibre 自己拉
      stylePromise = null;
      console.warn('[map] style prefetch failed, fallback to URL', err);
      return STYLE_URL;
    });
  return stylePromise;
}

/** 动态 import MapLibre，与样式并行 */
export function prefetchMapLibre() {
  if (maplibrePromise) return maplibrePromise;
  maplibrePromise = import('maplibre-gl');
  // 同步拉 css（已在组件内 import，这里再触发一次无妨）
  import('maplibre-gl/dist/maplibre-gl.css').catch(() => {});
  return maplibrePromise;
}

/** 应用空闲时预热（首页不阻塞首屏） */
export function scheduleMapWarmup() {
  if (typeof window === 'undefined') return;
  preconnectMapCdn();
  const run = () => {
    prefetchMapStyle();
    prefetchMapLibre();
  };
  if ('requestIdleCallback' in window) {
    window.requestIdleCallback(run, { timeout: 2500 });
  } else {
    window.setTimeout(run, 1200);
  }
}

/**
 * 解析最终 style：优先缓存对象
 * @returns {Promise<string|object>}
 */
export async function resolveMapStyle() {
  if (stylePromise) {
    try {
      return await stylePromise;
    } catch {
      return STYLE_URL;
    }
  }
  return prefetchMapStyle();
}
