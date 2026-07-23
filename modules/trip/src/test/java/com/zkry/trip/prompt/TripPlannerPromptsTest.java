package com.zkry.trip.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.zkry.trip.dto.RouteIntent;
import com.zkry.trip.dto.RouteNode;
import com.zkry.trip.dto.TripRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class TripPlannerPromptsTest {

    @Test
    void describesRouteOrderAndNodeRolesForPlanner() {
        TripRequest request = new TripRequest("杭州", null, "2026-08-01", "2026-08-01", 1,
            "公共交通", "舒适型酒店", "3000", List.of("湖景"), "", "zh", List.of(), List.of(),
            new RouteIntent("杭州", "soft_order", List.of(
                new RouteNode(1, "poi", "west-lake", "西湖", 120.1485, 30.242, "attraction", "傍晚看日落", List.of("必去", "拍照")),
                new RouteNode(2, "free_point", null, "自定义节点 2", 120.1152, 30.2288, null)
            )));

        String context = TripPlannerPrompts.requestVariables(request).get("route_context");

        assertThat(context).contains("柔性顺序", "01. 西湖", "命名地点", "偏好必去、拍照", "备注傍晚看日落", "02. 自定义节点 2", "希望经过的区域");
    }
}
