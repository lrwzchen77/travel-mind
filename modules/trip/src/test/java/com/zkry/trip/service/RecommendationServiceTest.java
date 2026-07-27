package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zkry.common.core.exception.BizException;
import com.zkry.trip.dto.ai.PythonAiCallResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class RecommendationServiceTest {

    @Test
    void fallbackFiltersPoiKindAndCapsTheLimit() {
        PythonAiClient ai = mock(PythonAiClient.class);
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("tm_user_preference"), any(Map.class))).thenReturn(List.of());
        when(ai.recommend(any())).thenReturn(PythonAiCallResult.failure("offline"));
        when(jdbc.queryForList(contains("kind = :type"), any(Map.class))).thenReturn(List.of(Map.of(
            "id", 8L, "name", "湖畔酒店", "city", "杭州", "address", "湖滨路",
            "rating", 4.5, "popularity", 90, "tags", "湖景,亲子")));

        var result = new RecommendationService(ai, jdbc).recommendForUser(1001L, "hotel", null, 999);

        assertThat(result).singleElement().satisfies(item -> assertThat(item.itemType()).isEqualTo("hotel"));
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).queryForList(contains("kind = :type"), params.capture());
        assertThat(params.getValue()).containsEntry("type", "hotel").containsEntry("limit", 50);
    }

    @Test
    void rejectsUnknownFeedbackBeforeWriting() {
        RecommendationService service = new RecommendationService(
            mock(PythonAiClient.class), mock(NamedParameterJdbcTemplate.class));

        assertThatThrownBy(() -> service.recordFeedback(1001L, 8L, "hotel", "arbitrary"))
            .isInstanceOf(BizException.class).hasMessage("推荐反馈无效。");
    }
}
