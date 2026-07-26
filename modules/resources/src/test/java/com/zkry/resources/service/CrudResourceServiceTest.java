package com.zkry.resources.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class CrudResourceServiceTest {

    @Test
    void rejectsInvalidManualPoiCoordinatesBeforeInsert() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        CrudResourceService service = new CrudResourceService(new CrudResourceRegistry(), jdbc);

        assertThatThrownBy(() -> service.create("map-pois", Map.of(
            "city", "杭州", "name", "非法坐标", "kind", "attraction", "longitude", 999, "latitude", 30
        ))).isInstanceOfSatisfying(ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
