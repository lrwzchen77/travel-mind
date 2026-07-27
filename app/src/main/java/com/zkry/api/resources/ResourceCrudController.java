package com.zkry.api.resources;

import com.zkry.common.core.domain.PageResult;
import com.zkry.common.core.domain.R;
import com.zkry.resources.service.CrudResourceService;
import com.zkry.resources.service.ResourceSearchCriteria;
import com.zkry.identity.service.IdentityService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/resources")
public class ResourceCrudController {

    private final CrudResourceService crudResourceService;
    private final IdentityService identityService;

    public ResourceCrudController(CrudResourceService crudResourceService, IdentityService identityService) {
        this.crudResourceService = crudResourceService;
        this.identityService = identityService;
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
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long attractionId,
        @RequestParam(required = false) Long targetId,
        @RequestParam(required = false) String targetType,
        @RequestParam(required = false) String analysisType,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        ResourceSearchCriteria criteria = ResourceSearchCriteria.of(keyword, cityId, category, tag, ratingMin,
            ratingMax, userId, attractionId, targetId, targetType, analysisType, status, pageNum, pageSize);
        return R.ok(crudResourceService.list(resourceKey, criteria));
    }

    @GetMapping("/{resourceKey}/{id}")
    public R<Map<String, Object>> detail(@PathVariable String resourceKey, @PathVariable long id) {
        return R.ok(crudResourceService.detail(resourceKey, id));
    }

    @PostMapping("/{resourceKey}")
    public R<Map<String, Object>> create(@PathVariable String resourceKey, @RequestBody Map<String, Object> payload) {
        if ("users".equals(resourceKey)) return R.ok(identityService.provision(payload));
        return R.ok(crudResourceService.create(resourceKey, payload));
    }

    @PutMapping("/{resourceKey}/{id}")
    public R<Map<String, Object>> update(
        @PathVariable String resourceKey,
        @PathVariable long id,
        @RequestBody Map<String, Object> payload
    ) {
        return R.ok(crudResourceService.update(resourceKey, id, payload));
    }

    @PutMapping("/{resourceKey}/{id}/status")
    public R<Map<String, Object>> updateStatus(
        @PathVariable String resourceKey,
        @PathVariable long id,
        @RequestParam int status
    ) {
        return R.ok(crudResourceService.updateStatus(resourceKey, id, status));
    }

    @DeleteMapping("/{resourceKey}/{id}")
    public R<Void> delete(@PathVariable String resourceKey, @PathVariable long id) {
        if ("users".equals(resourceKey)) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "用户账号只能停用，不能通过通用接口删除");
        }
        crudResourceService.delete(resourceKey, id);
        return R.ok();
    }
}
