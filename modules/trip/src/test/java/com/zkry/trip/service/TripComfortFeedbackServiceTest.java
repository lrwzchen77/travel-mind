package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zkry.common.core.exception.BizException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class TripComfortFeedbackServiceTest {

    @Test
    void rejectsFeedbackBeforeTripEnds() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(argThat(sql -> sql.contains("FROM tm_trip_plan")), anyMap()))
            .thenReturn(List.of(Map.of("id", 9001L, "end_date", Date.valueOf(LocalDate.now().plusDays(1)))));
        TripComfortFeedbackService service = new TripComfortFeedbackService(jdbc);

        assertThatThrownBy(() -> service.save(1001L, 9001L, Map.of("actual_label", "balanced")))
            .isInstanceOf(BizException.class)
            .hasMessage("行程结束后才能反馈实际体验。");
    }

    @Test
    void rejectsUnsupportedFeedbackLabelsAfterOwnershipCheck() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(argThat(sql -> sql.contains("FROM tm_trip_plan")), anyMap()))
            .thenReturn(List.of(Map.of("id", 9001L)));
        TripComfortFeedbackService service = new TripComfortFeedbackService(jdbc);

        assertThatThrownBy(() -> service.save(1001L, 9001L, Map.of("actual_label", "perfect")))
            .isInstanceOf(BizException.class)
            .hasMessage("舒适度反馈标签不支持。");
    }

    @Test
    void aggregatesOnlySupportedLabels() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(argThat(sql -> sql.contains("GROUP BY actual_label")), anyMap()))
            .thenReturn(List.of(Map.of("actual_label", "balanced", "count", 3L), Map.of("actual_label", "intense", "count", 2L)));
        TripComfortFeedbackService service = new TripComfortFeedbackService(jdbc);

        Map<String, Object> result = service.stats();

        assertThat(result.get("total")).isEqualTo(5L);
        assertThat(result.get("labels")).isEqualTo(Map.of("balanced", 3L, "intense", 2L, "relaxed", 0L));
    }
}
