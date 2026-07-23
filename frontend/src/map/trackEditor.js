const EARTH_RADIUS_KM = 6371;

export const MAX_TRACK_NODES = 30;
export const ROUTE_INTENT_KEY = 'travelmind.route-intent';

export function appendTrackPoint(points, lngLat, metadata = {}) {
  if (!Number.isFinite(lngLat?.lng) || !Number.isFinite(lngLat?.lat) || points.length >= MAX_TRACK_NODES
    || (metadata.poiId && points.some((point) => String(point.poiId) === String(metadata.poiId)))) return points;
  return [...points, {
    number: points.length + 1,
    longitude: Number(lngLat.lng.toFixed(6)),
    latitude: Number(lngLat.lat.toFixed(6)),
    ...(metadata.poiId ? { poiId: metadata.poiId } : {}),
    ...(metadata.name ? { name: metadata.name } : {}),
    ...(metadata.kind ? { kind: metadata.kind } : {}),
    ...(metadata.note ? { note: String(metadata.note).trim().slice(0, 240) } : {}),
    ...(Array.isArray(metadata.preferences) ? { preferences: metadata.preferences.slice(0, 6) } : {}),
  }];
}

export function interpolatePoint(from, to, progress) {
  const t = Math.max(0, Math.min(1, progress));
  return [
    from.longitude + (to.longitude - from.longitude) * t,
    from.latitude + (to.latitude - from.latitude) * t,
  ];
}

export function trackDistanceKm(points) {
  let distance = 0;
  for (let index = 1; index < points.length; index += 1) {
    const from = points[index - 1];
    const to = points[index];
    const dLat = toRadians(to.latitude - from.latitude);
    const dLng = toRadians(to.longitude - from.longitude);
    const a = Math.sin(dLat / 2) ** 2
      + Math.cos(toRadians(from.latitude)) * Math.cos(toRadians(to.latitude)) * Math.sin(dLng / 2) ** 2;
    distance += EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }
  return Math.round(distance * 10) / 10;
}

export function normalizeTrackPoints(nodes) {
  if (!Array.isArray(nodes)) return [];
  const poiIds = new Set();
  return nodes.slice(0, MAX_TRACK_NODES).flatMap((node) => {
    const longitude = Number(node?.longitude);
    const latitude = Number(node?.latitude);
    if (!Number.isFinite(longitude) || longitude < -180 || longitude > 180
      || !Number.isFinite(latitude) || latitude < -90 || latitude > 90) return [];
    const poiId = String(node.poi_id ?? node.poiId ?? '').trim().slice(0, 120);
    if (poiId && poiIds.has(poiId)) return [];
    if (poiId) poiIds.add(poiId);
    const name = String(node.name || '').trim().slice(0, 120);
    const kind = String(node.kind || '').trim().slice(0, 40);
    const note = String(node.note || '').trim().slice(0, 240);
    const preferences = [...new Set(Array.isArray(node.preferences) ? node.preferences : [])]
      .map((item) => String(item).trim().slice(0, 20)).filter(Boolean).slice(0, 6);
    return [{
      number: 0,
      longitude: Number(longitude.toFixed(6)),
      latitude: Number(latitude.toFixed(6)),
      ...(poiId ? { poiId } : {}),
      ...(name ? { name } : {}),
      ...(kind ? { kind } : {}),
      ...(note ? { note } : {}),
      ...(preferences.length ? { preferences } : {}),
    }];
  }).map((node, index) => ({ ...node, number: index + 1 }));
}

export function routeIntentFromTrack(city, points, mode = 'soft_order') {
  const nodes = normalizeTrackPoints(points);
  return {
    city: String(city || '').trim().slice(0, 60),
    mode: mode === 'strict_order' ? 'strict_order' : 'soft_order',
    nodes: nodes.map((node, index) => ({
      order: index + 1,
      type: node.poiId ? 'poi' : 'free_point',
      ...(node.poiId ? { poi_id: node.poiId } : {}),
      name: node.name || `自定义节点 ${index + 1}`,
      longitude: node.longitude,
      latitude: node.latitude,
      ...(node.kind ? { kind: node.kind } : {}),
      ...(node.note ? { note: node.note } : {}),
      ...(node.preferences?.length ? { preferences: node.preferences } : {}),
    })),
  };
}

export function normalizeRouteIntent(value, expectedCity = '') {
  if (!value || !['soft_order', 'strict_order'].includes(value.mode)) return null;
  const city = String(value.city || '').trim().slice(0, 60);
  if (!city || (expectedCity && city !== String(expectedCity).trim())) return null;
  const points = normalizeTrackPoints(value.nodes);
  if (points.length < 2 || points.length > MAX_TRACK_NODES) return null;
  return routeIntentFromTrack(city, points, value.mode);
}

function toRadians(value) {
  return value * Math.PI / 180;
}
