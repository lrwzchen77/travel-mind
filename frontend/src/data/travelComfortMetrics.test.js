import { describe, expect, it } from 'vitest';
import metrics from './travelComfortMetrics.json';

describe('TravelComfort 训练证据', () => {
  it('keeps published metrics consistent with the independent test set', () => {
    const total = metrics.confusion_matrix.flat().reduce((sum, value) => sum + value, 0);
    const correct = metrics.confusion_matrix.reduce((sum, row, index) => sum + row[index], 0);

    expect(metrics.features).toHaveLength(12);
    expect(metrics.learning_curve).toHaveLength(180);
    expect({ total, correct }).toEqual({ total: 1500, correct: 1247 });
    expect(metrics.accuracy).toBeCloseTo(correct / total, 4);
  });
});
