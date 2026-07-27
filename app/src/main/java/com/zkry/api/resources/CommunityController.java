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
import org.springframework.web.bind.annotation.PutMapping;

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
        return R.ok(communityService.post(id, viewerId()));
    }

    @GetMapping("/api/public/inspirations/{postId}/comments")
    public R<PageResult<Map<String, Object>>> comments(@PathVariable long postId,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(communityService.comments(postId, viewerId(), pageNum, pageSize));
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

    @PutMapping("/api/user/inspirations/posts/{postId}")
    public R<Map<String, Object>> update(@PathVariable long postId, @RequestBody Map<String, Object> payload) {
        return R.ok(communityService.updatePost(LoginHelper.getUserId(), postId, payload));
    }

    @PostMapping("/api/user/inspirations/posts/{postId}/submit")
    public R<Map<String, Object>> submit(@PathVariable long postId) {
        return R.ok(communityService.submitPost(LoginHelper.getUserId(), postId));
    }

    @PostMapping("/api/admin/inspirations/{postId}/review")
    public R<Map<String, Object>> review(@PathVariable long postId, @RequestBody ReviewRequest request) {
        return R.ok(communityService.reviewPost(LoginHelper.getUserId(), postId, request.status(), request.reason()));
    }

    public record ReviewRequest(int status, String reason) { }

    @PostMapping("/api/user/memories/{memoryId}/publish")
    public R<Map<String, Object>> publishMemory(@PathVariable long memoryId, @RequestBody Map<String, Object> payload) {
        return R.ok(communityService.publishMemory(LoginHelper.getUserId(), memoryId, payload));
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

    @PostMapping("/api/user/inspirations/{postId}/likes")
    public R<Map<String, Object>> like(@PathVariable long postId) {
        return R.ok(communityService.like(LoginHelper.getUserId(), postId));
    }

    @DeleteMapping("/api/user/inspirations/{postId}/likes")
    public R<Map<String, Object>> unlike(@PathVariable long postId) {
        return R.ok(communityService.unlike(LoginHelper.getUserId(), postId));
    }

    @PostMapping("/api/user/inspirations/{postId}/comments")
    public R<Map<String, Object>> createComment(@PathVariable long postId, @RequestBody Map<String, Object> payload) {
        return R.ok(communityService.createComment(LoginHelper.getUserId(), postId, payload));
    }

    @DeleteMapping("/api/user/inspirations/comments/{commentId}")
    public R<Void> deleteComment(@PathVariable long commentId) {
        communityService.deleteComment(LoginHelper.getUserId(), commentId);
        return R.ok();
    }

    private Long viewerId() {
        return LoginHelper.isLogin() ? LoginHelper.getUserId() : null;
    }
}
