package com.zkry.trip.dto.ai;

import java.util.List;
import java.util.Map;

public record TripEvaluateResult(
    String model_mode,
    String model_version,
    String comfort_class,
    Double confidence,
    Map<String, Double> probabilities,
    Map<String, Object> feature_snapshot,
    String training_source,
    Integer comfort_score,
    String risk_level,
    List<DailyRisk> daily_risks,
    List<String> suggestions
) {
}
