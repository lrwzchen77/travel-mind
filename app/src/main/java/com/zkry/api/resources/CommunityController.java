package com.zkry.api.resources;

import com.zkry.common.core.domain.PageResult;
import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.resources.service.CommunityService;
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
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/api/public/inspirations")
    public R<PageResult<Map<String, Object>>> posts(
        @RequestParam(required = false) String keyword, @RequestParam(required = false) String city,
        @RequestParam(required = false) String topic, @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return R.ok(communityService.posts(keyword, city, topic, pageNum, pageSize));
    }

    @GetMapping("/api/public/inspirations/{id}")
    public R<Map<String, Object>> post(@PathVariable long id) {
        return R.ok(communityService.post(id, null));
    }

    @GetMapping("/api/user/inspirations/posts")
    public R<PageResult<Map<String, Object>>> myPosts(@RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(communityService.myPosts(LoginHelper.getUserId(), pageNum, pageSize));
    }

    @PostMapping("/api/user/inspirations/posts")
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> payload) {
        return R.ok(communityService.createPost(LoginHelper.getUserId(), payload));
    }

    @GetMapping("/api/user/inspirations/bag")
    public R<PageResult<Map<String, Object>>> bag() {
        return R.ok(communityService.bag(LoginHelper.getUserId()));
    }

    @PostMapping("/api/user/inspirations/bag")
    public R<Map<String, Object>> addToBag(@RequestBody Map<String, Object> payload) {
        Object id = payload == null ? null : payload.get("post_id");
        if (!(id instanceof Number number)) throw new IllegalArgumentException("post_id 必填。");
        String intent = String.valueOf(payload.getOrDefault("intent", "reference"));
        return R.ok(communityService.addToBag(LoginHelper.getUserId(), number.longValue(), intent));
    }

    @DeleteMapping("/api/user/inspirations/bag/{postId}")
    public R<Void> removeFromBag(@PathVariable long postId) {
        communityService.removeFromBag(LoginHelper.getUserId(), postId);
        return R.ok();
    }
}
