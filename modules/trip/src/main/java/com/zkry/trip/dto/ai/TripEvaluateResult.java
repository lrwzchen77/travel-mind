package com.zkry.trip.dto.ai;

import java.util.List;

public record TripEvaluateResult(
    Integer comfort_score,
    String risk_level,
    List<DailyRisk> daily_risks,
    List<String> suggestions
) {
}
