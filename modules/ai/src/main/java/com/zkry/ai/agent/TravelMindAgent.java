package com.zkry.ai.agent;

/**
 * Travel Mind 内部 Agent 名称注册表。
 *
 * <p>Spring AI Alibaba 的 ReactAgent 需要一个 name。之前如果到处写字符串，
 * 改名或排查日志会很痛苦；现在所有 Agent 都从这里取 id。
 */
public enum TravelMindAgent {

    XHS_EXTRACTION("xhs-extraction-agent"),
    TRAVEL_RESEARCH("travel-research-agent"),
    TRIP_PLANNER("trip-planner-agent"),
    TRIP_REVIEW("trip-review-agent"),
    TRIP_CHAT("trip-chat-agent");

    private final String id;

    TravelMindAgent(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
