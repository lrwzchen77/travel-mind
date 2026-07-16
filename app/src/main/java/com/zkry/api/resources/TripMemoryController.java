package com.zkry.api.resources;

import com.zkry.common.core.domain.PageResult;
import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.resources.service.TripMemoryService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class TripMemoryController {

    private final TripMemoryService tripMemoryService;

    public TripMemoryController(TripMemoryService tripMemoryService) {
        this.tripMemoryService = tripMemoryService;
    }

    @PostMapping("/trips/{tripId}/memory")
    public R<Map<String, Object>> create(@PathVariable long tripId) {
        return R.ok(tripMemoryService.createFromTrip(LoginHelper.getUserId(), tripId));
    }

    @GetMapping("/memories")
    public R<PageResult<Map<String, Object>>> list(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return R.ok(tripMemoryService.list(LoginHelper.getUserId(), pageNum, pageSize));
    }

    @GetMapping("/memories/{memoryId}")
    public R<Map<String, Object>> detail(@PathVariable long memoryId) {
        return R.ok(tripMemoryService.detail(LoginHelper.getUserId(), memoryId));
    }

    @PostMapping("/memories/{memoryId}/items/photos")
    public R<Map<String, Object>> addPhoto(@PathVariable long memoryId, @RequestBody Map<String, Object> payload) {
        return R.ok(tripMemoryService.addPhoto(LoginHelper.getUserId(), memoryId, payload));
    }

    @DeleteMapping("/memories/{memoryId}/items/{itemId}")
    public R<Void> deleteItem(@PathVariable long memoryId, @PathVariable long itemId) {
        tripMemoryService.deleteItem(LoginHelper.getUserId(), memoryId, itemId);
        return R.ok();
    }

    @DeleteMapping("/memories/{memoryId}")
    public R<Void> delete(@PathVariable long memoryId) {
        tripMemoryService.delete(LoginHelper.getUserId(), memoryId);
        return R.ok();
    }
}
