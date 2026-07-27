package com.zkry.api.trip;

import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.trip.dto.ai.RecommendResult;
import com.zkry.trip.service.RecommendationService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能推荐引擎接口。
 */
@RestController
@RequestMapping("/api/user/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public R<List<RecommendResult>> recommend(
        @RequestParam(defaultValue = "city") String type,
        @RequestParam(required = false) String city,
        @RequestParam(defaultValue = "10") int limit
    ) {
        return R.ok(recommendationService.recommendForUser(LoginHelper.getUserId(), type, city, limit));
    }

    @PostMapping("/{id}/feedback")
    public R<Void> feedback(
        @PathVariable long id,
        @RequestParam String type,
        @RequestParam String feedback
    ) {
        recommendationService.recordFeedback(LoginHelper.getUserId(), id, type, feedback);
        return R.ok();
    }

    @PostMapping("/reindex")
    public R<Map<String, Object>> reindex() {
        int indexed = recommendationService.reindexDestinations();
        return R.ok(Map.of("indexed", indexed));
    }
}
