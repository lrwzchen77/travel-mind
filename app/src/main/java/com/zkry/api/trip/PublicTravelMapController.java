package com.zkry.api.trip;

import com.zkry.common.json.utils.JsonUtils;
import com.zkry.common.redis.util.RedisUtils;
import com.zkry.map.dto.PublicTravelMapSnapshot;
import com.zkry.map.service.PublicTravelDataService;
import com.zkry.resources.service.MapPoiCatalogService;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/travel-map")
public class PublicTravelMapController {

    private static final Logger log = LoggerFactory.getLogger(PublicTravelMapController.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private final PublicTravelDataService publicTravelDataService;
    private final MapPoiCatalogService mapPoiCatalogService;
    private final RedisUtils redis;

    public PublicTravelMapController(
        PublicTravelDataService publicTravelDataService, MapPoiCatalogService mapPoiCatalogService, RedisUtils redis
    ) {
        this.publicTravelDataService = publicTravelDataService;
        this.mapPoiCatalogService = mapPoiCatalogService;
        this.redis = redis;
    }

    @GetMapping
    public PublicTravelMapSnapshot map(
        @RequestParam String city,
        @RequestParam(required = false) Double longitude,
        @RequestParam(required = false) Double latitude
    ) {
        String cacheKey = "travelmind:public-map:%s:%s:%s".formatted(city.trim(), longitude, latitude);
        try {
            PublicTravelMapSnapshot cached = JsonUtils.parseObject(redis.getString(cacheKey), PublicTravelMapSnapshot.class);
            if (cached != null) return cached;
        } catch (RuntimeException ex) {
            log.warn("旅行地图 Redis 读取失败，继续回源 city={} reason={}", city, ex.getMessage());
        }
        PublicTravelMapSnapshot snapshot = publicTravelDataService.collectMap(city, longitude, latitude);
        double centerLongitude = longitude == null ? snapshot.places().stream().findFirst()
            .map(PublicTravelMapSnapshot.Place::longitude).orElse(0.0) : longitude;
        double centerLatitude = latitude == null ? snapshot.places().stream().findFirst()
            .map(PublicTravelMapSnapshot.Place::latitude).orElse(0.0) : latitude;
        PublicTravelMapSnapshot result = snapshot.withPlaces(mapPoiCatalogService.rememberAndList(
            snapshot.city(), centerLongitude, centerLatitude, snapshot.places()));
        try {
            redis.setString(cacheKey, JsonUtils.toJsonString(result), CACHE_TTL);
        } catch (RuntimeException ex) {
            log.warn("旅行地图 Redis 写入失败，当前请求继续返回 city={} reason={}", city, ex.getMessage());
        }
        return result;
    }
}
