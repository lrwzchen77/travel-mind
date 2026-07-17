package com.zkry.api.trip;

import com.zkry.map.dto.PublicTravelMapSnapshot;
import com.zkry.map.service.PublicTravelDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/travel-map")
public class PublicTravelMapController {

    private final PublicTravelDataService publicTravelDataService;

    public PublicTravelMapController(PublicTravelDataService publicTravelDataService) {
        this.publicTravelDataService = publicTravelDataService;
    }

    @GetMapping
    public PublicTravelMapSnapshot map(@RequestParam String city) {
        return publicTravelDataService.collectMap(city);
    }
}
