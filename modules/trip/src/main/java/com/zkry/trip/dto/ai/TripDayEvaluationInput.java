package com.zkry.trip.dto.ai;

import java.util.List;

public record TripDayEvaluationInput(
    String date,
    String city,
    List<String> attractions,
    String weather,
    Boolean transfer
) {
}
