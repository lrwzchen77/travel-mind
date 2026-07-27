package com.zkry.api.identity;

import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.identity.service.IdentityService;
import com.zkry.resources.service.UserProfileService;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final IdentityService identityService;
    private final UserProfileService profileService;

    public AccountController(IdentityService identityService, UserProfileService profileService) {
        this.identityService = identityService;
        this.profileService = profileService;
    }

    @PutMapping("/user/account/password")
    public R<Void> changePassword(@RequestBody PasswordChange request) {
        identityService.changePassword(LoginHelper.getUserId(), request.currentPassword(), request.newPassword());
        return R.ok();
    }

    @GetMapping("/user/account/export")
    public R<Map<String, Object>> exportData() {
        return R.ok(profileService.exportData(LoginHelper.getUserId()));
    }

    @DeleteMapping("/user/account")
    public R<Void> deactivate() {
        identityService.deactivate(LoginHelper.getUserId());
        return R.ok();
    }

    @PutMapping("/admin/users/{userId}/password")
    public R<Void> resetPassword(@PathVariable long userId, @RequestBody PasswordReset request) {
        identityService.resetPassword(userId, request.newPassword());
        return R.ok();
    }

    @PutMapping("/admin/users/{userId}/role")
    public R<Void> updateRole(@PathVariable long userId, @RequestBody RoleChange request) {
        identityService.updateRole(userId, request.role());
        return R.ok();
    }

    public record PasswordChange(String currentPassword, String newPassword) { }
    public record PasswordReset(String newPassword) { }
    public record RoleChange(String role) { }
}
