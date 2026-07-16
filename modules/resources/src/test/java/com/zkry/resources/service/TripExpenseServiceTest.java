package com.zkry.resources.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.zkry.common.core.exception.BizException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class TripExpenseServiceTest {

    @Test
    void createsExpenseOnlyForOwnedTrip() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("tm_trip_plan"), any(Map.class))).thenReturn(List.of(Map.of("budget", new BigDecimal("1000"))));
        when(jdbc.update(contains("INSERT INTO tm_trip_expense"), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class))).thenReturn(1);
        when(jdbc.queryForList(contains("tm_trip_expense"), any(Map.class))).thenReturn(List.of());
        when(jdbc.queryForObject(contains("COALESCE(SUM(amount)"), any(Map.class), org.mockito.ArgumentMatchers.eq(BigDecimal.class))).thenReturn(new BigDecimal("88.50"));
        TripExpenseService service = new TripExpenseService(jdbc);

        Map<String, Object> summary = service.create(1001L, 9001L, Map.of("category", "food", "title", "午餐", "amount", "88.5", "spent_on", "2026-08-01"));

        verify(jdbc).update(contains("INSERT INTO tm_trip_expense"), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class));
        assertThat(summary).containsEntry("budget", new BigDecimal("1000")).containsEntry("actual", new BigDecimal("88.50"))
            .containsEntry("remaining", new BigDecimal("911.50"));
    }

    @Test
    void rejectsInvalidAmountBeforeWriting() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("tm_trip_plan"), any(Map.class))).thenReturn(List.of(Map.of("budget", BigDecimal.ZERO)));
        TripExpenseService service = new TripExpenseService(jdbc);

        assertThatThrownBy(() -> service.create(1001L, 9001L, Map.of("category", "food", "title", "午餐", "amount", "0")))
            .isInstanceOf(BizException.class).hasMessage("金额需大于 0 且不超过 100 万。");
        assertThatThrownBy(() -> service.create(1001L, 9001L, Map.of("category", "food", "title", "午餐", "amount", "1.234")))
            .isInstanceOf(BizException.class).hasMessage("金额最多保留两位小数。");
        assertThatThrownBy(() -> service.create(1001L, 9001L, Map.of("category", "unknown", "title", "午餐", "amount", "10")))
            .isInstanceOf(BizException.class).hasMessage("支出分类不支持。");
        assertThatThrownBy(() -> service.create(1001L, 9001L, Map.of("category", "food", "title", "午餐", "amount", "10", "spent_on", "2026-02-30")))
            .isInstanceOf(BizException.class).hasMessage("消费日期格式不正确。");
    }

    @Test
    void refusesExpensesForAnotherUsersTrip() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("tm_trip_plan"), any(Map.class))).thenReturn(List.of());
        TripExpenseService service = new TripExpenseService(jdbc);

        assertThatThrownBy(() -> service.summary(1002L, 9001L))
            .isInstanceOf(BizException.class).hasMessage("行程不存在或无权操作。");
        verify(jdbc, never()).update(any(String.class), any(Map.class));
    }

    @Test
    void reportsMissingExpenseInsteadOfPretendingDeletionSucceeded() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(contains("tm_trip_plan"), any(Map.class))).thenReturn(List.of(Map.of("budget", BigDecimal.ZERO)));
        when(jdbc.update(contains("UPDATE tm_trip_expense"), any(Map.class))).thenReturn(0);
        TripExpenseService service = new TripExpenseService(jdbc);

        assertThatThrownBy(() -> service.delete(1001L, 9001L, 7001L))
            .isInstanceOf(BizException.class).hasMessage("这笔花费不存在或已删除。");
    }
}
