/**
 * 本地飞航轨迹：大圆弧 / 二次抬升弧线（纯前端，无网络）
 */

/** 经纬度插值（线性，短距够用） */
export function lerpLngLat(a, b, t) {
  return [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t];
}

/**
 * 生成带高度感的飞行弧线（平面上弯曲，像航线）
 * @param {[number, number]} from [lng, lat]
 * @param {[number, number]} to
 * @param {number} steps
 * @param {number} bulge 弧高系数（相对距离）
 */
export function buildFlightArc(from, to, steps = 64, bulge = 0.22) {
  const coords = [];
  const mid = lerpLngLat(from, to, 0.5);
  const dx = to[0] - from[0];
  const dy = to[1] - from[1];
  const dist = Math.hypot(dx, dy) || 0.01;
  // 垂直方向抬升控制点
  const nx = -dy / dist;
  const ny = dx / dist;
  const lift = dist * bulge;
  // 略向东北抬，避免弧线太平
  const ctrl = [mid[0] + nx * lift * 0.15, mid[1] + Math.abs(lift)];

  for (let i = 0; i <= steps; i += 1) {
    const t = i / steps;
    // 二次贝塞尔
    const u = 1 - t;
    const lng = u * u * from[0] + 2 * u * t * ctrl[0] + t * t * to[0];
    const lat = u * u * from[1] + 2 * u * t * ctrl[1] + t * t * to[1];
    coords.push([lng, lat]);
  }
  return coords;
}

export function arcFeature(from, to, props = {}) {
  return {
    type: 'Feature',
    properties: { ...props },
    geometry: {
      type: 'LineString',
      coordinates: buildFlightArc(from, to),
    },
  };
}

export function pointFeature(lng, lat, props = {}) {
  return {
    type: 'Feature',
    properties: props,
    geometry: { type: 'Point', coordinates: [lng, lat] },
  };
}

/** 城市底座：近似圆多边形，用于 fill-extrusion 立体柱 */
export function cityPadPolygon(lng, lat, radiusDeg = 0.12, sides = 20, height = 800) {
  const ring = [];
  for (let i = 0; i <= sides; i += 1) {
    const a = (i / sides) * Math.PI * 2;
    // 纬度修正，让圆不扁
    const cosLat = Math.cos((lat * Math.PI) / 180) || 0.7;
    ring.push([lng + Math.cos(a) * radiusDeg / cosLat, lat + Math.sin(a) * radiusDeg]);
  }
  return {
    type: 'Feature',
    properties: { height, base: 0 },
    geometry: { type: 'Polygon', coordinates: [ring] },
  };
}

export function emptyLineCollection() {
  return { type: 'FeatureCollection', features: [] };
}

export function lineCollection(coords, props = {}) {
  return {
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        properties: props,
        geometry: { type: 'LineString', coordinates: coords },
      },
    ],
  };
}

/** 取弧线上某一进度的点与朝向 */
export function sampleArc(coords, t) {
  if (!coords.length) return { point: [0, 0], bearing: 0 };
  const clamped = Math.min(1, Math.max(0, t));
  const f = clamped * (coords.length - 1);
  const i = Math.floor(f);
  const frac = f - i;
  const a = coords[i];
  const b = coords[Math.min(i + 1, coords.length - 1)];
  const point = lerpLngLat(a, b, frac);
  const bearing = (Math.atan2(b[0] - a[0], b[1] - a[1]) * 180) / Math.PI;
  return { point, bearing };
}
