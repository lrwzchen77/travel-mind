import cityPredictions from './cityVisionInsights.json';
import { cityImageSlugs } from './cityImages.js';

const labelMeta = {
  scenic_spot: { label: '城市风光', preference: '拍照', tone: 'scenic' },
  restaurant_food: { label: '在地美食', preference: '美食', tone: 'food' },
  hotel_room: { label: '住宿空间', preference: '轻松', tone: 'stay' },
  transport_station: { label: '交通场景', preference: '', tone: 'transit' },
  crowded_scene: { label: '可能拥挤', preference: '轻松', tone: 'risk' },
  low_light_scene: { label: '夜景画面', preference: '夜景', tone: 'night' },
};

export function visionLabelMeta(label) {
  return labelMeta[label] || { label: '旅行场景', preference: '', tone: 'default' };
}

export function formatVisionConfidence(value) {
  const confidence = Number(value);
  if (!Number.isFinite(confidence)) return '';
  const percent = confidence * 100;
  return percent >= 99.5 && percent < 100 ? `${percent.toFixed(1)}%` : `${Math.round(percent)}%`;
}

export function cityVisionInsight(city) {
  const slug = cityImageSlugs[city];
  const prediction = slug ? cityPredictions[slug] : null;
  if (!prediction || Number(prediction.confidence) < 0.75) return null;
  return {
    ...prediction,
    ...visionLabelMeta(prediction.label),
    confidenceText: formatVisionConfidence(prediction.confidence),
  };
}

export function buildVisionPlanningQuery({ city, prediction }) {
  const meta = visionLabelMeta(prediction?.name);
  return {
    city: String(city || '').trim(),
    preference: meta.preference || undefined,
    vision: meta.label,
  };
}
