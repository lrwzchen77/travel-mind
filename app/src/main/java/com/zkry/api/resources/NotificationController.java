package com.zkry.api.resources;

import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.resources.service.NotificationService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        return R.ok(notifications.list(LoginHelper.getUserId()));
    }

    @PostMapping("/{id}/read")
    public R<Void> read(@PathVariable long id) {
        notifications.read(LoginHelper.getUserId(), id);
        return R.ok();
    }

    @PostMapping("/read-all")
    public R<Void> readAll() {
        notifications.readAll(LoginHelper.getUserId());
        return R.ok();
    }
}
