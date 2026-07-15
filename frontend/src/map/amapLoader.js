/**
 * 动态加载高德地图 JS API 2.0
 * 控制台申请：https://console.amap.com/dev/key/app
 * Key 类型选「Web端(JS API)」，并配置安全密钥 securityJsCode
 */

const AMAP_KEY = import.meta.env.VITE_AMAP_KEY || '';
const AMAP_SECURITY = import.meta.env.VITE_AMAP_SECURITY_CODE || '';

let loadPromise = null;

export function hasAmapKey() {
  return Boolean(AMAP_KEY && String(AMAP_KEY).trim() && !String(AMAP_KEY).includes('your_'));
}

export function getAmapKeyHint() {
  return {
    key: AMAP_KEY,
    configured: hasAmapKey(),
    consoleUrl: 'https://console.amap.com/dev/key/app',
  };
}

export function loadAmap() {
  if (typeof window !== 'undefined' && window.AMap) {
    return Promise.resolve(window.AMap);
  }
  if (!hasAmapKey()) {
    return Promise.reject(new Error('未配置 VITE_AMAP_KEY'));
  }
  if (loadPromise) return loadPromise;

  if (!AMAP_SECURITY || String(AMAP_SECURITY).includes('your_')) {
    return Promise.reject(
      new Error('未配置 VITE_AMAP_SECURITY_CODE（高德 JS API 2.0 必须配合安全密钥）'),
    );
  }

  loadPromise = new Promise((resolve, reject) => {
    // 必须在加载 maps 脚本之前设置，否则会报「需要配合安全密钥」
    window._AMapSecurityConfig = {
      securityJsCode: String(AMAP_SECURITY).trim(),
    };

    const existing = document.querySelector('script[data-amap-sdk="2.0"]');
    if (existing && window.AMap) {
      resolve(window.AMap);
      return;
    }

    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.async = true;
    script.dataset.amapSdk = '2.0';
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(AMAP_KEY)}&plugin=AMap.Scale,AMap.ToolBar,AMap.ControlBar,AMap.MoveAnimation`;
    script.onload = () => {
      if (window.AMap) resolve(window.AMap);
      else {
        loadPromise = null;
        reject(new Error('高德脚本已加载但 AMap 不可用'));
      }
    };
    script.onerror = () => {
      loadPromise = null;
      reject(new Error('高德地图脚本加载失败，请检查网络、Key 与安全密钥'));
    };
    document.head.appendChild(script);
  });

  return loadPromise;
}

/** Travel Mind 主题向的高德标准样式 */
export const AMAP_THEME_STYLE = 'amap://styles/whitesmoke';
