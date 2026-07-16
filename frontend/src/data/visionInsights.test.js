import { describe, expect, it } from 'vitest';
import {
  buildVisionPlanningQuery,
  cityVisionInsight,
  formatVisionConfidence,
  visionLabelMeta,
} from './visionInsights.js';

describe('vision insights', () => {
  it('maps trained classes to user-facing travel language', () => {
    expect(visionLabelMeta('crowded_scene')).toMatchObject({ label: '可能拥挤', preference: '轻松' });
    expect(visionLabelMeta('low_light_scene')).toMatchObject({ label: '夜景画面', preference: '夜景' });
    expect(formatVisionConfidence(0.9394)).toBe('94%');
  });

  it('only exposes confident precomputed city-cover predictions', () => {
    expect(cityVisionInsight('成都')).toMatchObject({ label: '城市风光', confidenceText: '99.8%' });
    expect(cityVisionInsight('杭州')).toBeNull();
  });

  it('builds a planning handoff without claiming city recognition', () => {
    expect(buildVisionPlanningQuery({
      city: '重庆',
      prediction: { name: 'low_light_scene' },
    })).toEqual({ city: '重庆', preference: '夜景', vision: '夜景画面' });
  });
});
