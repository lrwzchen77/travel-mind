package com.zkry.api.resources;

import com.zkry.common.core.domain.PageResult;
import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.resources.service.CrudResourceService;
import com.zkry.resources.service.ResourceSearchCriteria;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/user/library")
public class UserLibraryController {

    private static final Set<String> USER_RESOURCES = Set.of("favorites", "travel-notes", "ai-records");
    private final CrudResourceService crudResourceService;

    public UserLibraryController(CrudResourceService crudResourceService) {
        this.crudResourceService = crudResourceService;
    }

    @GetMapping("/{resourceKey}")
    public R<PageResult<Map<String, Object>>> list(
        @PathVariable String resourceKey,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        requireUserResource(resourceKey);
        return R.ok(crudResourceService.list(resourceKey, ResourceSearchCriteria.of(
            keyword, null, null, null, null, null, LoginHelper.getUserId(), null, null,
            null, null, null, pageNum, pageSize)));
    }

    @PostMapping("/{resourceKey}")
    public R<Map<String, Object>> create(@PathVariable String resourceKey, @RequestBody Map<String, Object> payload) {
        requireWritable(resourceKey);
        Map<String, Object> ownedPayload = writablePayload(resourceKey, payload);
        ownedPayload.put("user_id", LoginHelper.getUserId());
        if ("travel-notes".equals(resourceKey)) {
            ownedPayload.put("visibility", "private");
            ownedPayload.put("status", 1);
        }
        return R.ok(crudResourceService.create(resourceKey, ownedPayload));
    }

    @PutMapping("/{resourceKey}/{id}")
    public R<Map<String, Object>> update(
        @PathVariable String resourceKey,
        @PathVariable long id,
        @RequestBody Map<String, Object> payload
    ) {
        if (!"travel-notes".equals(resourceKey)) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Resource is not editable");
        }
        requireWritable(resourceKey);
        Map<String, Object> existing = requireOwner(resourceKey, id);
        Map<String, Object> values = writablePayload(resourceKey, payload);
        if ("travel-notes".equals(resourceKey) && "public".equals(existing.get("visibility"))) {
            values.put("status", 0);
            values.put("review_reason", null);
        }
        return R.ok(crudResourceService.update(resourceKey, id, values));
    }

    @DeleteMapping("/{resourceKey}/{id}")
    public R<Void> delete(@PathVariable String resourceKey, @PathVariable long id) {
        requireWritable(resourceKey);
        requireOwner(resourceKey, id);
        crudResourceService.delete(resourceKey, id);
        return R.ok();
    }

    private Map<String, Object> requireOwner(String resourceKey, long id) {
        Map<String, Object> resource = crudResourceService.detail(resourceKey, id);
        Object userId = resource.get("user_id");
        long ownerId = userId instanceof Number number ? number.longValue() : -1L;
        if (ownerId != LoginHelper.getUserId()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        return resource;
    }

    private void requireWritable(String resourceKey) {
        requireUserResource(resourceKey);
        if ("ai-records".equals(resourceKey)) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Resource is read-only");
        }
    }

    private void requireUserResource(String resourceKey) {
        if (!USER_RESOURCES.contains(resourceKey)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
    }

    private Map<String, Object> writablePayload(String resourceKey, Map<String, Object> payload) {
        Set<String> allowed = "favorites".equals(resourceKey)
            ? Set.of("target_type", "target_id", "note")
            : Set.of("city_id", "attraction_id", "title", "content");
        Map<String, Object> result = new LinkedHashMap<>();
        if (payload != null) payload.forEach((key, value) -> {
            if (allowed.contains(key)) result.put(key, value);
        });
        return result;
    }
}
