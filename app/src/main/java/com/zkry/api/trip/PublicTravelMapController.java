package com.zkry.api.trip;

import com.zkry.map.dto.PublicTravelMapSnapshot;
import com.zkry.map.service.PublicTravelDataService;
import com.zkry.resources.service.MapPoiCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/travel-map")
public class PublicTravelMapController {

    private final PublicTravelDataService publicTravelDataService;
    private final MapPoiCatalogService mapPoiCatalogService;

    public PublicTravelMapController(
        PublicTravelDataService publicTravelDataService, MapPoiCatalogService mapPoiCatalogService
    ) {
        this.publicTravelDataService = publicTravelDataService;
        this.mapPoiCatalogService = mapPoiCatalogService;
    }

    @GetMapping
    public PublicTravelMapSnapshot map(
        @RequestParam String city,
        @RequestParam(required = false) Double longitude,
        @RequestParam(required = false) Double latitude
    ) {
        PublicTravelMapSnapshot snapshot = publicTravelDataService.collectMap(city, longitude, latitude);
        double centerLongitude = longitude == null ? snapshot.places().stream().findFirst()
            .map(PublicTravelMapSnapshot.Place::longitude).orElse(0.0) : longitude;
        double centerLatitude = latitude == null ? snapshot.places().stream().findFirst()
            .map(PublicTravelMapSnapshot.Place::latitude).orElse(0.0) : latitude;
        return snapshot.withPlaces(mapPoiCatalogService.rememberAndList(
            snapshot.city(), centerLongitude, centerLatitude, snapshot.places()));
    }
}
