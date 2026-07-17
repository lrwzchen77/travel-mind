package com.zkry.map.dto;

public record PublicDataItem(
    String title,
    String detail,
    String source,
    String updated_at,
    String data_kind,
    boolean bookable,
    String url
) {
}
