package com.zkry.trip.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.zkry.common.core.config.TravelMindRuntimeSettingsService;
import com.zkry.common.core.exception.BizException;
import com.zkry.common.redis.util.RedisUtils;
import com.zkry.trip.dto.TripRequest;
import com.zkry.resources.service.NotificationService;
import com.zkry.trip.dto.RouteIntent;
import com.zkry.trip.dto.RouteNode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TripTaskServiceValidationTest {

    private final TripTaskService service = new TripTaskService(
        mock(TripAiPlannerService.class),
        mock(TripResearchService.class),
        mock(TravelMindRuntimeSettingsService.class),
        mock(DemoTripPlannerService.class),
        mock(TripPlanPersistenceService.class),
        mock(TripPlanReviewer.class),
        mock(TravelAiApplicationService.class),
        mock(NotificationService.class),
        mock(RedisUtils.class),
        false, 1, 10, 30
    );

    @Test
    void acceptsConsistentFutureDates() {
        LocalDate start = LocalDate.now().plusDays(7);
        assertThatCode(() -> service.validateTripRequest(request(start, start.plusDays(2), 3)))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsPastOrInconsistentDates() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        assertThatThrownBy(() -> service.validateTripRequest(request(yesterday, yesterday, 1)))
            .isInstanceOf(BizException.class)
            .hasMessage("出发日期不能早于今天。");

        LocalDate start = LocalDate.now().plusDays(7);
        assertThatThrownBy(() -> service.validateTripRequest(request(start, start.plusDays(3), 2)))
            .isInstanceOf(BizException.class)
            .hasMessage("返程日期与旅行天数不一致。");
    }

    @Test
    void validatesStructuredRouteAtRequestBoundary() {
        LocalDate start = LocalDate.now().plusDays(7);
        TripRequest valid = new TripRequest("杭州", null, start.toString(), start.toString(), 1,
            "公共交通", "舒适型酒店", "3000", List.of("轻松"), "", "zh", List.of(), List.of(),
            new RouteIntent("杭州", "soft_order", List.of(
                new RouteNode(1, "poi", "west-lake", "西湖", 120.1485, 30.242, "attraction"),
                new RouteNode(2, "free_point", null, "自定义节点 2", 120.1152, 30.2288, null)
            )));
        assertThatCode(() -> service.validateTripRequest(valid)).doesNotThrowAnyException();

        TripRequest wrongCity = new TripRequest("杭州", null, start.toString(), start.toString(), 1,
            "公共交通", "舒适型酒店", "3000", List.of(), "", "zh", List.of(), List.of(),
            new RouteIntent("成都", "soft_order", valid.route_intent().nodes()));
        assertThatThrownBy(() -> service.validateTripRequest(wrongCity))
            .isInstanceOf(BizException.class)
            .hasMessage("路线草稿与目的地城市不一致。");
    }

    private TripRequest request(LocalDate start, LocalDate end, int days) {
        return new TripRequest("杭州", null, start.toString(), end.toString(), days,
            "公共交通", "舒适型酒店", "3000", List.of("轻松"), "", "zh");
    }
}
