package com.zkry.trip.dto;

import java.util.List;

public record RouteIntent(
    String city,
    String mode,
    List<RouteNode> nodes
) {
    public List<RouteNode> safeNodes() {
        return nodes == null ? List.of() : nodes;
    }

    public String safeMode() {
        return "strict_order".equals(mode) ? "strict_order" : "soft_order";
    }
}
