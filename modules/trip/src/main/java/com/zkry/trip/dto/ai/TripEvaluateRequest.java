package com.zkry.trip.dto.ai;

import java.util.List;

public record TripEvaluateRequest(
    List<TripDayEvaluationInput> days,
    String transportation,
    Integer city_transfers,
    List<String> preferences,
    Double budget
) {
}
