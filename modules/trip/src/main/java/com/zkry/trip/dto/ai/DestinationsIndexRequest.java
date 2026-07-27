package com.zkry.trip.dto.ai;

import java.util.List;

public record DestinationsIndexRequest(
    List<DestinationItemInput> destinations
) {
}
