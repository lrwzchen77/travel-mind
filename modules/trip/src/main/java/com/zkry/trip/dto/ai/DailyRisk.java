package com.zkry.trip.dto.ai;

import java.util.List;

public record DailyRisk(
    Integer day_index,
    String date,
    String city,
    Integer attractions_count,
    List<String> risk_items
) {
}
