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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
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
    void unpublishedContentCannotEnterAnInspirationBag() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("SELECT n.id, n.user_id"), any(Map.class))).thenReturn(List.of());
        CommunityService service = new CommunityService(jdbc);

        assertThatThrownBy(() -> service.addToBag(1001L, 7001L, "must"))
            .isInstanceOf(BizException.class).hasMessage("社区分享不存在或暂不可见。");
        verify(jdbc, never()).update(contains("tm_inspiration_item"), any(Map.class));
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
}
