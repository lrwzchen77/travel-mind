import { describe, expect, it } from 'vitest';
import metrics from './travelRiskYoloMetrics.json';

describe('TravelRisk-YOLO 训练证据', () => {
  it('keeps the published test accuracy consistent with the confusion matrix', () => {
    const matrix = metrics.splits.test.confusion_matrix;
    const total = matrix.flat().reduce((sum, value) => sum + value, 0);
    const correct = matrix.reduce((sum, row, index) => sum + row[index], 0);

    expect(metrics.splits.test.labels).toHaveLength(6);
    expect({ total, correct }).toEqual({ total: 99, correct: 93 });
    expect(metrics.test_accuracy).toBeCloseTo(correct / total, 4);
  });
});
