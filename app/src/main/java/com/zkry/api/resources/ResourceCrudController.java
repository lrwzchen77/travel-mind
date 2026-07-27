package com.zkry.api.resources;

import com.zkry.common.core.domain.PageResult;
import com.zkry.common.core.domain.R;
import com.zkry.resources.service.CrudResourceService;
import com.zkry.resources.service.ResourceSearchCriteria;
import com.zkry.identity.service.IdentityService;
import com.zkry.common.core.exception.BizException;
import java.util.Map;
import java.util.LinkedHashMap;
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
        if (!"users".equals(resourceKey) || payload == null || !payload.containsKey("status")) {
            return R.ok(crudResourceService.update(resourceKey, id, payload));
        }
        Map<String, Object> profile = new LinkedHashMap<>(payload);
        int status = status(profile.remove("status"));
        if (!profile.isEmpty()) crudResourceService.update(resourceKey, id, profile);
        identityService.updateStatus(id, status);
        return R.ok(crudResourceService.detail(resourceKey, id));
    }

    @PutMapping("/{resourceKey}/{id}/status")
    public R<Map<String, Object>> updateStatus(
        @PathVariable String resourceKey,
        @PathVariable long id,
        @RequestParam int status
    ) {
        if ("travel-notes".equals(resourceKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请使用内容审核接口并填写审核结论");
        }
        if ("users".equals(resourceKey)) {
            identityService.updateStatus(id, status);
            return R.ok(crudResourceService.detail(resourceKey, id));
        }
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

    private int status(Object value) {
        try {
            int status = value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
            if (status != 0 && status != 1) throw new NumberFormatException();
            return status;
        } catch (NumberFormatException ex) {
            throw new BizException("账号状态无效。");
        }
    }
}
