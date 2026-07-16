package com.zkry.api.trip;

import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.resources.service.TripExpenseService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/trip/{tripId}/expenses")
public class TripExpenseController {

    private final TripExpenseService tripExpenseService;

    public TripExpenseController(TripExpenseService tripExpenseService) {
        this.tripExpenseService = tripExpenseService;
    }

    @GetMapping
    public R<Map<String, Object>> summary(@PathVariable long tripId) {
        return R.ok(tripExpenseService.summary(LoginHelper.getUserId(), tripId));
    }

    @PostMapping
    public R<Map<String, Object>> create(@PathVariable long tripId, @RequestBody Map<String, Object> payload) {
        return R.ok(tripExpenseService.create(LoginHelper.getUserId(), tripId, payload));
    }

    @DeleteMapping("/{expenseId}")
    public R<Void> delete(@PathVariable long tripId, @PathVariable long expenseId) {
        tripExpenseService.delete(LoginHelper.getUserId(), tripId, expenseId);
        return R.ok();
    }
}
