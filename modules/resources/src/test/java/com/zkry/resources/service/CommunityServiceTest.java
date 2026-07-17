package com.zkry.resources.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zkry.common.core.exception.BizException;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class CommunityServiceTest {

    @Test
    void publicPostsWaitForReviewWhilePrivatePostsStayPersonal() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("SELECT id FROM tm_city"), any(Map.class))).thenReturn(List.of(Map.of("id", 101L)));
        when(jdbc.queryForList(contains("SELECT n.id, n.user_id"), any(Map.class))).thenReturn(List.of(Map.of(
            "id", 7001L, "title", "西湖慢游", "content", "少走路路线", "visibility", "public", "status", 0
        )));
        CommunityService service = new CommunityService(jdbc);

        service.createPost(1001L, Map.of("title", "西湖慢游", "content", "少走路路线", "topic", "route", "city", "杭州", "visibility", "public"));
        service.createPost(1001L, Map.of("title", "私藏早餐", "content", "自己的早餐清单", "topic", "food", "visibility", "private"));

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc, times(2)).update(contains("INSERT INTO tm_travel_note"), params.capture());
        assertThat(params.getAllValues().get(0).getValue("userId")).isEqualTo(1001L);
        assertThat(params.getAllValues().get(0).getValue("visibility")).isEqualTo("public");
        assertThat(params.getAllValues().get(0).getValue("status")).isEqualTo(0);
        assertThat(params.getAllValues().get(1).getValue("visibility")).isEqualTo("private");
        assertThat(params.getAllValues().get(1).getValue("status")).isEqualTo(1);
    }

    @Test
    void publicDiscoveryAlwaysRequiresApprovedPublicContent() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(any(String.class), any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(0L);
        when(jdbc.queryForList(any(String.class), any(MapSqlParameterSource.class))).thenReturn(List.of());
        CommunityService service = new CommunityService(jdbc);

        service.posts("西湖", "杭州", "route", 1, 20);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture(), any(MapSqlParameterSource.class));
        assertThat(sql.getValue()).contains("n.visibility = 'public'", "n.status = 1", "n.deleted = 0");
    }

    @Test
    void unpublishedContentCannotBeCollectedLikedOrCommented() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("SELECT n.id, n.user_id"), any(Map.class))).thenReturn(List.of());
        CommunityService service = new CommunityService(jdbc);

        assertThatThrownBy(() -> service.addToBag(1001L, 7001L, "must"))
            .isInstanceOf(BizException.class).hasMessage("社区分享不存在或暂不可见。");
        assertThatThrownBy(() -> service.like(1001L, 7001L))
            .isInstanceOf(BizException.class).hasMessage("社区分享不存在或暂不可见。");
        assertThatThrownBy(() -> service.createComment(1001L, 7001L, Map.of("content", "看起来不错")))
            .isInstanceOf(BizException.class).hasMessage("社区分享不存在或暂不可见。");
        verify(jdbc, never()).update(contains("tm_inspiration_item"), any(Map.class));
        verify(jdbc, never()).update(contains("tm_travel_note_like"), any(Map.class));
        verify(jdbc, never()).update(contains("tm_travel_note_comment"), any(Map.class));
    }

    @Test
    void ownedPostListingIsScopedAndPageSizeIsCapped() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(contains("user_id = :userId"), eq(Map.of("userId", 1001L)), eq(Long.class))).thenReturn(1L);
        when(jdbc.queryForList(contains("WHERE n.user_id = :userId"), any(MapSqlParameterSource.class))).thenReturn(List.of());
        CommunityService service = new CommunityService(jdbc);

        service.myPosts(1001L, 0, 999);

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForList(contains("WHERE n.user_id = :userId"), params.capture());
        assertThat(params.getValue().getValue("userId")).isEqualTo(1001L);
        assertThat(params.getValue().getValue("limit")).isEqualTo(30);
        assertThat(params.getValue().getValue("offset")).isEqualTo(0);
    }

    @Test
    void repeatedLikesAreSafeAndUnlikeIsScopedToCurrentUser() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("SELECT n.id, n.title"), any(Map.class)))
            .thenReturn(List.of(Map.of("id", 7001L)));
        when(jdbc.queryForObject(contains("WHERE travel_note_id = :postId"), any(Map.class), eq(Long.class)))
            .thenReturn(1L);
        CommunityService service = new CommunityService(jdbc);

        service.like(1001L, 7001L);
        service.like(1001L, 7001L);
        service.unlike(1001L, 7001L);

        verify(jdbc, times(2)).update(contains("INSERT IGNORE INTO tm_travel_note_like"),
            eq(Map.of("postId", 7001L, "userId", 1001L)));
        verify(jdbc).update(contains("DELETE FROM tm_travel_note_like"),
            eq(Map.of("postId", 7001L, "userId", 1001L)));
    }

    @Test
    void blankOrOversizedCommentsAreRejectedBeforeInsert() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("SELECT n.id, n.title"), any(Map.class)))
            .thenReturn(List.of(Map.of("id", 7001L)));
        CommunityService service = new CommunityService(jdbc);

        assertThatThrownBy(() -> service.createComment(1001L, 7001L, Map.of("content", "  ")))
            .isInstanceOf(BizException.class).hasMessage("请填写评论内容。");
        assertThatThrownBy(() -> service.createComment(1001L, 7001L, Map.of("content", "x".repeat(1001))))
            .isInstanceOf(BizException.class).hasMessage("content 内容过长。");
        verify(jdbc, never()).update(contains("INSERT INTO tm_travel_note_comment"), any(Map.class));
    }

    @Test
    void onlyTheCommentOwnerCanDeleteFromPublishedPosts() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.update(contains("UPDATE tm_travel_note_comment"), any(Map.class))).thenReturn(1, 0);
        CommunityService service = new CommunityService(jdbc);

        service.deleteComment(1001L, 9001L);
        assertThatThrownBy(() -> service.deleteComment(1002L, 9001L))
            .isInstanceOf(BizException.class).hasMessage("评论不存在或无权删除。");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(2)).update(sql.capture(), any(Map.class));
        assertThat(sql.getValue()).contains("cm.user_id = :userId", "n.visibility = 'public'", "n.status = 1");
    }

    @Test
    void memoryPublicationUsesOwnedSafeFactsWithoutPrivateIdentifiersOrExpenses() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("FROM tm_trip_memory\n"), any(Map.class))).thenReturn(List.of(Map.of(
            "title", "杭州两日游", "destination_city", "杭州")));
        when(jdbc.queryForList(contains("item_type IN ('place', 'photo')"), any(Map.class))).thenReturn(List.of(
            Map.of("item_type", "place", "day_index", 1, "place_name", "西湖", "ai_caption", ""),
            Map.of("item_type", "photo", "day_index", 1, "place_name", "断桥", "ai_caption", "西湖，风景区，照片")));
        when(jdbc.queryForList(contains("SELECT id FROM tm_city"), any(Map.class))).thenReturn(List.of(Map.of("id", 101L)));
        CommunityService service = new CommunityService(jdbc);

        Map<String, Object> result = service.publishMemory(1001L, 3001L,
            Map.of("title", "我的杭州慢游", "note", "清晨绕湖走很舒服", "tags", "湖景 慢游"));

        assertThat(result).containsEntry("visibility", "public").containsEntry("status", 0);
        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(contains("INSERT INTO tm_travel_note"), params.capture());
        String content = String.valueOf(params.getValue().getValue("content"));
        assertThat(content).contains("来自真实行程", "Day 1 · 西湖", "Day 1 · 断桥")
            .doesNotContain("3001", "memory", "evidence", "经度", "纬度", "¥");
        assertThat(params.getValue().getValue("tags")).isEqualTo("湖景，慢游，真实行程");
    }

    @Test
    void memoryPublicationRejectsOtherOwnersAndPrivateDetails() {
        NamedParameterJdbcTemplate denied = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(denied.queryForList(contains("FROM tm_trip_memory\n"), any(Map.class))).thenReturn(List.of());
        CommunityService deniedService = new CommunityService(denied);
        assertThatThrownBy(() -> deniedService.publishMemory(1002L, 3001L, Map.of()))
            .isInstanceOf(BizException.class).hasMessage("旅行记忆不存在或无权发布。");

        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("FROM tm_trip_memory\n"), any(Map.class))).thenReturn(List.of(Map.of(
            "title", "杭州两日游", "destination_city", "杭州")));
        CommunityService service = new CommunityService(jdbc);
        assertThatThrownBy(() -> service.publishMemory(1001L, 3001L, Map.of("note", "午餐花了 188 元")))
            .isInstanceOf(BizException.class).hasMessage("公开内容不能包含消费金额、精确坐标或 GPS 信息。");
        verify(jdbc, never()).update(contains("INSERT INTO tm_travel_note"), any(MapSqlParameterSource.class));
    }

    @Test
    @ResourceLock("user.dir")
    void memoryPublicationReencodesTheOwnedCoverInsteadOfPublishingOriginalMetadata(@TempDir Path temp) throws Exception {
        String previous = System.getProperty("user.dir");
        Path uploads = Files.createDirectories(temp.resolve("uploads"));
        String sourceName = "123e4567-e89b-12d3-a456-426614174000.jpg";
        ImageIO.write(new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB), "jpg", uploads.resolve(sourceName).toFile());
        System.setProperty("user.dir", temp.toString());
        try {
            NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
            when(jdbc.queryForList(any(String.class), any(Map.class))).thenAnswer(call -> {
                String sql = call.getArgument(0);
                if (sql.contains("FROM tm_trip_memory\n")) return List.of(Map.of("title", "杭州旅行", "destination_city", "杭州"));
                if (sql.contains("item_type IN ('place', 'photo')")) return List.of();
                if (sql.contains("SELECT i.source_url")) return List.of(Map.of("source_url", "/uploads/" + sourceName));
                if (sql.contains("SELECT id FROM tm_city")) return List.of(Map.of("id", 101L));
                return List.of();
            });
            CommunityService service = new CommunityService(jdbc);

            Map<String, Object> numeric = service.publishMemory(1001L, 3001L,
                Map.of("photo_item_id", 996889308694955193L));
            Map<String, Object> string = service.publishMemory(1001L, 3001L,
                Map.of("photo_item_id", "996889308694955193"));

            for (Map<String, Object> result : List.of(numeric, string)) {
                String cover = String.valueOf(result.get("cover_image"));
                assertThat(cover).startsWith("/uploads/").endsWith(".png").doesNotContain(sourceName);
                Path publicFile = uploads.resolve(Path.of(cover).getFileName());
                assertThat(publicFile).exists();
                assertThat(ImageIO.read(publicFile.toFile())).isNotNull();
                Files.delete(publicFile);
            }
        } finally {
            System.setProperty("user.dir", previous);
        }
    }

    @Test
    void memoryPublicationRejectsInvalidStringPhotoIds() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("FROM tm_trip_memory\n"), any(Map.class))).thenReturn(List.of(Map.of(
            "title", "杭州旅行", "destination_city", "杭州")));
        when(jdbc.queryForList(contains("item_type IN ('place', 'photo')"), any(Map.class))).thenReturn(List.of());
        CommunityService service = new CommunityService(jdbc);

        for (String value : List.of("", "0", "12x", "9223372036854775808")) {
            assertThatThrownBy(() -> service.publishMemory(1001L, 3001L, Map.of("photo_item_id", value)))
                .isInstanceOf(BizException.class).hasMessage("公开封面选择无效。");
        }
        verify(jdbc, never()).update(contains("INSERT INTO tm_travel_note"), any(MapSqlParameterSource.class));
    }

    @Test
    void publicCoverDimensionsAreBoundedBeforeDecodingPixels() {
        CommunityService.validatePublicCoverDimensions(8_000, 5_000);

        assertThatThrownBy(() -> CommunityService.validatePublicCoverDimensions(16_385, 1))
            .isInstanceOf(BizException.class).hasMessageContaining("尺寸过大");
        assertThatThrownBy(() -> CommunityService.validatePublicCoverDimensions(8_001, 5_000))
            .isInstanceOf(BizException.class).hasMessageContaining("尺寸过大");
    }
}
