package com.zkry.trip.dto.ai;

import java.util.List;

public record VisionDetectResult(
    String model_mode,
    List<AiLabel> labels,
    List<String> scene_tags,
    String summary,
    List<String> risk_hints,
    String source
) {
}
