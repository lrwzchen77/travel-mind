package com.zkry.resources.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Java 与旅行记忆 AI 服务之间的最小、可追溯数据契约。 */
public final class TripMemoryAnalysisContract {

    private TripMemoryAnalysisContract() {
    }

    public record Input(long memoryId, long tripId, String title, String destinationCity, List<ItemInput> items) {
    }

    public record ItemInput(
        long itemId,
        String itemType,
        String sourceUrl,
        LocalDateTime takenAt,
        BigDecimal latitude,
        BigDecimal longitude,
        String city,
        String placeName,
        String content,
        Integer dayIndex
    ) {
    }

    public record Result(List<ItemResult> items, Generation generation) {
    }

    public record ItemResult(
        long itemId,
        String caption,
        List<String> tags,
        String placeName,
        BigDecimal confidence,
        LocalDateTime takenAt,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer dayIndex,
        Long matchedItemId,
        List<String> evidenceReasons,
        String scene,
        List<String> riskHints,
        String modelMode,
        Integer orientation,
        String imageStatus
    ) {
        public ItemResult(long itemId, String caption, List<String> tags, String placeName, BigDecimal confidence) {
            this(itemId, caption, tags, placeName, confidence, null, null, null, null, null,
                List.of(), null, List.of(), null, null, null);
        }
    }

    public record Generation(String type, String content, List<Long> evidenceItemIds) {
    }

    public record Saved(long generationId, int version) {
    }
}
