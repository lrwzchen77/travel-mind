package com.zkry.api.resources;

import com.zkry.common.core.domain.R;
import com.zkry.resources.service.UserProfileService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public R<Map<String, Object>> profile(@RequestParam(defaultValue = "1001") long userId) {
        return R.ok(userProfileService.profile(userId));
    }

    @PutMapping
    public R<Map<String, Object>> updateProfile(
        @RequestParam(defaultValue = "1001") long userId,
        @RequestBody Map<String, Object> payload
    ) {
        return R.ok(userProfileService.updateProfile(userId, payload));
    }
}
