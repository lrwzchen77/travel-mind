package com.zkry.api.resources;

import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.resources.service.UserProfileService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public R<Map<String, Object>> profile() {
        return R.ok(userProfileService.profile(LoginHelper.getUserId()));
    }

    @PutMapping
    public R<Map<String, Object>> updateProfile(
        @RequestBody Map<String, Object> payload
    ) {
        return R.ok(userProfileService.updateProfile(LoginHelper.getUserId(), payload));
    }
}
