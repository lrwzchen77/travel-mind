function unwrap(value) {
  let current = value;
  for (let index = 0; index < 3 && current && typeof current === 'object' && !current.model_mode; index += 1) {
    current = current.data;
  }
  return current || {};
}

export function visionRecordSummary(record = {}) {
  let raw = record.result_json;
  try {
    if (typeof raw === 'string') raw = JSON.parse(raw);
  } catch {
    raw = {};
  }
  const result = unwrap(raw);
  const firstLabel = result.labels?.[0] || {};
  return {
    id: record.id,
    createdAt: record.created_at || record.createdAt || '',
    modelMode: result.model_mode || '',
    label: firstLabel.name || '',
    confidence: Number(firstLabel.confidence || 0),
  };
}

export function modelRunSummary(records = []) {
  const runs = records.map(visionRecordSummary).filter((item) => item.modelMode);
  return {
    total: records.length,
    trained: runs.filter((item) => item.modelMode === 'trained_yolo').length,
    fallback: runs.filter((item) => item.modelMode === 'rule').length,
    latest: runs[0] || null,
  };
}
