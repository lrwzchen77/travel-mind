package com.zkry.resources.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zkry.common.core.exception.BizException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class TripMemoryServiceTest {

    @Test
    void createsIdempotentPrivateMemoryAndSeedsAvailableTripFacts() {
        NamedParameterJdbcTemplate jdbc = jdbcForOwnedMemory();
        TripMemoryService service = new TripMemoryService(jdbc);

        Map<String, Object> first = service.createFromTrip(1001L, 9001L);
        Map<String, Object> second = service.createFromTrip(1001L, 9001L);

        assertThat(first).containsEntry("id", 3001L).containsEntry("visibility", "private");
        assertThat(second).containsEntry("id", 3001L);
        verify(jdbc, times(2)).update(argThat(sql -> sql.contains("INSERT INTO tm_trip_memory\n")), any(MapSqlParameterSource.class));
        verify(jdbc, atLeast(6)).update(contains("INSERT IGNORE INTO tm_trip_memory_item"), any(MapSqlParameterSource.class));
        ArgumentCaptor<MapSqlParameterSource> memoryInsert = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc, times(2)).update(contains("visibility"), memoryInsert.capture());
        assertThat(memoryInsert.getValue().getValues()).containsEntry("userId", 1001L).containsEntry("tripId", 9001L);
    }

    @Test
    void refusesAnotherUsersTripAndMemoryBeforeWriting() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("FROM tm_trip_plan"), any(Map.class))).thenReturn(List.of());
        TripMemoryService service = new TripMemoryService(jdbc);

        assertThatThrownBy(() -> service.createFromTrip(1002L, 9001L))
            .isInstanceOf(BizException.class).hasMessage("行程不存在或无权操作。");
        assertThatThrownBy(() -> service.detail(1002L, 3001L))
            .isInstanceOf(BizException.class).hasMessage("旅行记忆不存在或无权访问。");
        verify(jdbc, never()).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void refusesMemoryBeforeTheTripEnds() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("FROM tm_trip_plan"), any(Map.class))).thenReturn(List.of(Map.of(
            "id", 9001L, "title", "未来行程", "destination_city", "杭州", "summary", "待出发", "has_ended", 0)));
        TripMemoryService service = new TripMemoryService(jdbc);

        assertThatThrownBy(() -> service.createFromTrip(1001L, 9001L))
            .isInstanceOf(BizException.class).hasMessage("行程结束后才能创建旅行记忆。");
        verify(jdbc, never()).update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void refusesReadAndDeleteForAnotherUsersMemory() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("FROM tm_trip_memory WHERE id"), any(Map.class))).thenReturn(List.of());
        when(jdbc.update(contains("DELETE FROM tm_trip_memory WHERE"), any(Map.class))).thenReturn(0);
        TripMemoryService service = new TripMemoryService(jdbc);

        assertThatThrownBy(() -> service.detail(1002L, 3001L)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.delete(1002L, 3001L))
            .isInstanceOf(BizException.class).hasMessage("旅行记忆不存在或无权删除。");
    }

    @Test
    @ResourceLock("user.dir")
    void acceptsOnlyExistingControlledUploadAndStoresRelativePath(@TempDir Path temp) throws Exception {
        String previous = System.getProperty("user.dir");
        String name = "123e4567-e89b-12d3-a456-426614174000.jpg";
        Files.createDirectories(temp.resolve("uploads"));
        Files.writeString(temp.resolve("uploads").resolve(name), "image");
        System.setProperty("user.dir", temp.toString());
        try {
            NamedParameterJdbcTemplate jdbc = jdbcForPhoto();
            TripMemoryService service = new TripMemoryService(jdbc);
            Map<String, Object> photo = service.addPhoto(1001L, 3001L,
                Map.of("url", "http://localhost:8080/uploads/" + name, "latitude", "30.1", "longitude", "120.2"));

            assertThat(photo).containsEntry("source_url", "/uploads/" + name);
            ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
            verify(jdbc).update(contains("INSERT INTO tm_trip_memory_item"), params.capture());
            assertThat(params.getValue().getValue("sourceUrl")).isEqualTo("/uploads/" + name);
            assertThatThrownBy(() -> service.addPhoto(1001L, 3001L, Map.of("url", "https://example.com/photo.jpg")))
                .isInstanceOf(BizException.class).hasMessage("照片地址不是受控上传路径。");
            assertThatThrownBy(() -> service.addPhoto(1001L, 3001L,
                Map.of("url", "/uploads/123e4567-e89b-12d3-a456-426614174999.png")))
                .isInstanceOf(BizException.class).hasMessage("上传的照片不存在。");
        } finally {
            System.setProperty("user.dir", previous);
        }
    }

    @Test
    void itemAndMemoryDeletionAreOwnershipScoped() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("FROM tm_trip_memory WHERE id"), any(Map.class)))
            .thenReturn(List.of(memoryRow()));
        when(jdbc.update(contains("DELETE FROM tm_trip_memory_item"), any(Map.class))).thenReturn(1);
        when(jdbc.update(contains("DELETE FROM tm_trip_memory WHERE"), any(Map.class))).thenReturn(1);
        TripMemoryService service = new TripMemoryService(jdbc);

        service.deleteItem(1001L, 3001L, 4001L);
        service.delete(1001L, 3001L);

        verify(jdbc).update(contains("id = :itemId AND memory_id = :memoryId"), any(Map.class));
        verify(jdbc).update(contains("id = :memoryId AND user_id = :userId"), any(Map.class));
    }

    @Test
    void savesOnlyAnalysisItemsAndEvidenceOwnedByMemory() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("FROM tm_trip_memory WHERE id"), any(Map.class)))
            .thenReturn(List.of(memoryRow()));
        when(jdbc.update(contains("UPDATE tm_trip_memory_item SET ai_caption"), any(MapSqlParameterSource.class))).thenReturn(1);
        when(jdbc.queryForObject(contains("COUNT(1) FROM tm_trip_memory_item"), any(Map.class),
            org.mockito.ArgumentMatchers.eq(Long.class))).thenReturn(1L);
        when(jdbc.queryForObject(contains("MAX(version)"), any(Map.class),
            org.mockito.ArgumentMatchers.eq(Integer.class))).thenReturn(1);
        TripMemoryService service = new TripMemoryService(jdbc);
        var result = new TripMemoryAnalysisContract.Result(
            List.of(new TripMemoryAnalysisContract.ItemResult(4001L, "西湖边的照片", List.of("湖景"), "西湖", new BigDecimal("0.91"))),
            new TripMemoryAnalysisContract.Generation("timeline", "第一天游览西湖。", List.of(4001L)));

        TripMemoryAnalysisContract.Saved saved = service.saveAnalysis(1001L, 3001L, result);

        assertThat(saved.version()).isEqualTo(1);
        verify(jdbc).update(contains("INSERT INTO tm_trip_memory_generation"), any(Map.class));
    }

    @Test
    void rejectsCitationThatDoesNotBelongToOwnedMemory() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("FROM tm_trip_memory WHERE id"), any(Map.class)))
            .thenReturn(List.of(memoryRow()));
        when(jdbc.queryForList(contains("FROM tm_trip_memory_item\n"), any(Map.class))).thenReturn(List.of());
        TripMemoryService service = new TripMemoryService(jdbc);
        var answer = new TripMemoryKnowledgeContract.Answer("你去了别人的酒店。", List.of(
            new TripMemoryKnowledgeContract.Citation(9999L, "trip_item", 8888L, "敏感证据")), true);

        assertThatThrownBy(() -> service.validateAnswer(1001L, 3001L, answer))
            .isInstanceOf(BizException.class).hasMessage("旅行记忆回答包含不属于当前记忆册的引用。");
    }

    @Test
    void indexStatusIsOwnershipScopedAndRecordsReadyTime() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("FROM tm_trip_memory WHERE id"), any(Map.class)))
            .thenReturn(List.of(memoryRow()));
        TripMemoryService service = new TripMemoryService(jdbc);

        service.knowledgeStatus(1001L, 3001L, "ready");

        verify(jdbc).update(contains("indexed_at = CASE WHEN :status = 'ready'"),
            org.mockito.ArgumentMatchers.<Map<String, ?>>argThat(values ->
                "ready".equals(values.get("status")) && Long.valueOf(3001L).equals(values.get("memoryId"))));
    }

    private NamedParameterJdbcTemplate jdbcForOwnedMemory() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(anyString(), any(Map.class))).thenAnswer(call -> {
            String sql = call.getArgument(0);
            if (sql.contains("FROM tm_trip_plan")) return List.of(tripRow());
            if (sql.contains("FROM tm_trip_memory WHERE id")) return List.of(memoryRow());
            if (sql.contains("FROM tm_trip_item")) return List.of(Map.of(
                "id", 5001L, "item_order", 1, "title", "西湖", "location", "杭州", "note", "慢游", "cost", new BigDecimal("0"), "day_no", 1));
            if (sql.contains("FROM tm_trip_expense")) return List.of(Map.of(
                "id", 6001L, "title", "午餐", "category", "food", "amount", new BigDecimal("88"), "day_no", 1));
            return List.of();
        });
        when(jdbc.queryForMap(contains("SELECT id FROM tm_trip_memory"), any(Map.class))).thenReturn(Map.of("id", 3001L));
        return jdbc;
    }

    private NamedParameterJdbcTemplate jdbcForPhoto() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("FROM tm_trip_memory WHERE id"), any(Map.class))).thenReturn(List.of(memoryRow()));
        when(jdbc.queryForObject(contains("MAX(sort_order)"), any(Map.class),
            org.mockito.ArgumentMatchers.eq(Integer.class))).thenReturn(1);
        when(jdbc.queryForMap(contains("FROM tm_trip_memory_item WHERE id"), any(Map.class)))
            .thenAnswer(call -> Map.of("id", 4001L, "source_url", lastPhotoPath(call)));
        return jdbc;
    }

    private String lastPhotoPath(org.mockito.invocation.InvocationOnMock ignored) {
        return "/uploads/123e4567-e89b-12d3-a456-426614174000.jpg";
    }

    private Map<String, Object> tripRow() {
        return Map.of("id", 9001L, "title", "杭州两日游", "destination_city", "杭州", "summary", "慢游西湖", "has_ended", 1);
    }

    private Map<String, Object> memoryRow() {
        return Map.of("id", 3001L, "trip_plan_id", 9001L, "title", "杭州两日游", "destination_city", "杭州",
            "summary", "慢游西湖", "status", "draft", "visibility", "private", "generation_status", "pending");
    }
}
