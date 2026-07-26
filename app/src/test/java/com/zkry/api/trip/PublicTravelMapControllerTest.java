package com.zkry.api.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zkry.common.json.utils.JsonUtils;
import com.zkry.common.redis.util.RedisUtils;
import com.zkry.map.dto.PublicTravelMapSnapshot;
import com.zkry.map.service.PublicTravelDataService;
import com.zkry.resources.service.MapPoiCatalogService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicTravelMapControllerTest {

    @Test
    void reusesRedisSnapshotAndFallsBackToSourceOnMiss() {
        PublicTravelDataService source = org.mockito.Mockito.mock(PublicTravelDataService.class);
        MapPoiCatalogService catalog = org.mockito.Mockito.mock(MapPoiCatalogService.class);
        RedisUtils redis = org.mockito.Mockito.mock(RedisUtils.class);
        PublicTravelMapController controller = new PublicTravelMapController(source, catalog, redis);
        PublicTravelMapSnapshot cached = PublicTravelMapSnapshot.empty("杭州");
        when(redis.getString(startsWith("travelmind:public-map:杭州"))).thenReturn(JsonUtils.toJsonString(cached));

        assertThat(controller.map("杭州", null, null)).isEqualTo(cached);
        verify(source, never()).collectMap(any(), any(), any());

        when(redis.getString(startsWith("travelmind:public-map:北京"))).thenReturn(null);
        PublicTravelMapSnapshot fresh = PublicTravelMapSnapshot.empty("北京");
        when(source.collectMap("北京", null, null)).thenReturn(fresh);
        when(catalog.rememberAndList("北京", 0.0, 0.0, List.of())).thenReturn(List.of());

        assertThat(controller.map("北京", null, null).city()).isEqualTo("北京");
        verify(redis).setString(startsWith("travelmind:public-map:北京"), any(), any(Duration.class));
    }
}
