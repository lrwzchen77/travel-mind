package com.zkry.api.resources;

import com.zkry.common.core.domain.PageResult;
import com.zkry.common.core.domain.R;
import com.zkry.resources.service.CrudResourceService;
import com.zkry.resources.service.ResourceSearchCriteria;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/resources")
public class PublicResourceController {

    private static final Set<String> DISCOVERY_RESOURCES = Set.of("cities", "attractions", "hotels", "restaurants", "travel-tags");

    private final CrudResourceService crudResourceService;

    public PublicResourceController(CrudResourceService crudResourceService) {
        this.crudResourceService = crudResourceService;
    }

    @GetMapping("/{resourceKey}")
    public R<PageResult<Map<String, Object>>> list(
        @PathVariable String resourceKey,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long cityId,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) Double ratingMin,
        @RequestParam(required = false) Double ratingMax,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        requireDiscoveryResource(resourceKey);
        ResourceSearchCriteria criteria = ResourceSearchCriteria.of(
            keyword, cityId, category, tag, ratingMin, ratingMax,
            null, null, null, null, null, "1", pageNum, pageSize);
        return R.ok(crudResourceService.list(resourceKey, criteria));
    }

    @GetMapping("/{resourceKey}/{id}")
    public R<Map<String, Object>> detail(@PathVariable String resourceKey, @PathVariable long id) {
        requireDiscoveryResource(resourceKey);
        Map<String, Object> resource = crudResourceService.detail(resourceKey, id);
        Object status = resource.get("status");
        if (status != null && (!(status instanceof Number number) || number.intValue() != 1)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        return R.ok(resource);
    }

    private void requireDiscoveryResource(String resourceKey) {
        if (!DISCOVERY_RESOURCES.contains(resourceKey)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
    }
}
