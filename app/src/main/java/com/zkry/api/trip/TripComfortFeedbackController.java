package com.zkry.api.trip;

import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.trip.service.TripComfortFeedbackService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TripComfortFeedbackController {

    private final TripComfortFeedbackService feedbackService;

    public TripComfortFeedbackController(TripComfortFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/api/user/ai/trip/{tripId}/comfort/feedback")
    public R<Map<String, Object>> latest(@PathVariable long tripId) {
        return R.ok(feedbackService.latest(LoginHelper.getUserId(), tripId));
    }

    @PostMapping("/api/user/ai/trip/{tripId}/comfort/feedback")
    public R<Map<String, Object>> save(@PathVariable long tripId, @RequestBody Map<String, Object> payload) {
        return R.ok(feedbackService.save(LoginHelper.getUserId(), tripId, payload));
    }

    @GetMapping("/api/admin/ai/travel-comfort/feedback/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(feedbackService.stats());
    }
}
