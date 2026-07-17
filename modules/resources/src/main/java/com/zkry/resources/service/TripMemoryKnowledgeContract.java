package com.zkry.resources.service;

import java.util.List;

/** Java 编排与 Python 私有旅行记忆检索之间的最小契约。 */
public final class TripMemoryKnowledgeContract {

    public static final String NO_EVIDENCE = "这次旅行记录里没有找到足够证据。";

    private TripMemoryKnowledgeContract() {
    }

    public record Source(long memoryId, long tripId, String title, String destinationCity, List<Item> items) {
    }

    public record Item(
        long itemId,
        String itemType,
        String sourceType,
        Long sourceId,
        String city,
        String placeName,
        String content,
        String aiCaption,
        List<String> aiTags,
        Integer dayIndex,
        boolean timelineEvidence
    ) {
    }

    public record IndexRequest(
        long memoryId,
        long tripId,
        String ownerScope,
        String title,
        String destinationCity,
        List<Item> items
    ) {
    }

    public record IndexResult(long memoryId, int indexedItems, String embeddingModel) {
    }

    public record QueryRequest(long memoryId, String ownerScope, String question, int topK) {
    }

    public record Citation(long memoryItemId, String sourceType, Long sourceId, String excerpt) {
    }

    public record Answer(String answer, List<Citation> citations, boolean fallback) {
    }

    public record DeleteRequest(long memoryId, String ownerScope) {
    }

    public record DeleteResult(long memoryId, boolean deleted) {
    }

    public record Identity(long memoryId, long tripId) {
    }
}
