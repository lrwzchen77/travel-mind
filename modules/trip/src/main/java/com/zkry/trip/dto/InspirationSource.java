package com.zkry.trip.dto;

/** 已审核社区帖子在一次规划中的不可变引用快照。 */
public record InspirationSource(
    Long post_id,
    String title,
    String city,
    String topic,
    String intent,
    String excerpt
) {
}
