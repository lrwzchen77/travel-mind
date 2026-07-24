import { describe, expect, it } from 'vitest';
import { modelRunSummary, visionRecordSummary } from './adminDashboard.js';

describe('admin dashboard model telemetry', () => {
  it('extracts YOLO mode and confidence from stored AI responses', () => {
    const record = { id: 7, result_json: JSON.stringify({ data: { model_mode: 'trained_yolo', labels: [{ name: 'crowded_scene', confidence: 0.9933 }] } }) };
    expect(visionRecordSummary(record)).toMatchObject({ id: 7, modelMode: 'trained_yolo', label: 'crowded_scene', confidence: 0.9933 });
    expect(modelRunSummary([record, { result_json: JSON.stringify({ data: { model_mode: 'rule' } }) }])).toMatchObject({ total: 2, trained: 1, fallback: 1 });
  });
});
