package com.zkry.resources.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import com.zkry.map.dto.PublicTravelMapSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class MapPoiCatalogServiceTest {

    @Test
    void deduplicatesByNameKeepsRicherSourceAndReturnsEveryPlace() {
        List<PublicTravelMapSnapshot.Place> places = new ArrayList<>();
        for (int i = 0; i < 22; i++) places.add(place("osm-" + i, "景点" + i, "attraction", i + 1, null, 0));
        places.add(place("osm-west-lake", "西 湖", "attraction", 1.0, null, 0));
        places.add(place("amap-west-lake", "西湖", "attraction", 1.2, 4.8, 3));
        places.add(place("hotel", "湖畔酒店", "hotel", 2.0, 4.6, 0));

        List<PublicTravelMapSnapshot.Place> ranked = MapPoiCatalogService.rank(places);

        assertThat(ranked).filteredOn(place -> "attraction".equals(place.kind())).hasSize(23);
        assertThat(ranked).filteredOn(place -> "hotel".equals(place.kind())).hasSize(1);
        assertThat(ranked).filteredOn(place -> place.name().replace(" ", "").equals("西湖"))
            .singleElement().extracting(PublicTravelMapSnapshot.Place::source).isEqualTo("高德地图");
        assertThat(ranked.get(0).name()).isEqualTo("西湖");
    }

    @Test
    void returnsLivePlacesWhenCatalogIsUnavailable() {
        NamedParameterJdbcTemplate jdbc = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        doThrow(new DataAccessResourceFailureException("offline"))
            .when(jdbc).batchUpdate(anyString(), any(Map[].class));
        MapPoiCatalogService service = new MapPoiCatalogService(jdbc);
        List<PublicTravelMapSnapshot.Place> live = List.of(place("one", "西湖", "attraction", 1, 4.8, 0));

        assertThat(service.rememberAndList("杭州", 120.1551, 30.2741, live)).containsExactlyElementsOf(live);
    }

    private PublicTravelMapSnapshot.Place place(
        String id, String name, String kind, double distance, Double rating, int mentions
    ) {
        return new PublicTravelMapSnapshot.Place(
            id, name, kind, 120.1551, 30.2741, "", "", "", distance,
            rating, null, "", "", mentions, "", id.startsWith("amap") ? "高德地图" : "OpenStreetMap", ""
        );
    }
}
