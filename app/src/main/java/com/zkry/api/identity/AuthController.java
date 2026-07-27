package com.zkry.api.identity;

import cn.dev33.satoken.stp.StpUtil;
import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.common.satoken.core.LoginUser;
import com.zkry.identity.domain.IdentityAccount;
import com.zkry.identity.service.IdentityService;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final IdentityService identityService;

    public AuthController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/user/auth/login")
    public R<LoginResponse> userLogin(@RequestBody LoginRequest request) {
        return R.ok(login(request, "user"));
    }

    @PostMapping("/user/auth/register")
    public R<LoginResponse> userRegister(@RequestBody RegisterRequest request) {
        identityService.register(request.username(), request.nickname(), request.password());
        return R.ok(login(new LoginRequest(request.username(), request.password()), "user"));
    }

    @PostMapping("/admin/auth/login")
    public R<LoginResponse> adminLogin(@RequestBody LoginRequest request) {
        return R.ok(login(request, "admin"));
    }

    @GetMapping({"/user/auth/me", "/admin/auth/me"})
    public R<SessionUser> me() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            IdentityAccount account = identityService.findByUserId(LoginHelper.getUserId());
            loginUser = toLoginUser(account);
        }
        return R.ok(toSessionUser(loginUser));
    }

    @PostMapping({"/user/auth/logout", "/admin/auth/logout"})
    public R<Void> logout() {
        LoginHelper.logout();
        return R.ok();
    }

    private LoginResponse login(LoginRequest request, String portalRole) {
        IdentityAccount account = identityService.authenticate(request.username(), request.password(), portalRole);
        LoginUser loginUser = toLoginUser(account);
        LoginHelper.login(loginUser);
        return new LoginResponse(
            StpUtil.getTokenName(),
            StpUtil.getTokenValue(),
            toSessionUser(loginUser)
        );
    }

    private LoginUser toLoginUser(IdentityAccount account) {
        return new LoginUser(account.userId(), account.nickname(), account.roles(), account.permissions(), account.authVersion());
    }

    private SessionUser toSessionUser(LoginUser loginUser) {
        return new SessionUser(loginUser.userId(), loginUser.username(), loginUser.roles(), loginUser.permissions());
    }

    public record LoginRequest(String username, String password) {
    }

    public record RegisterRequest(String username, String nickname, String password) {
    }

    public record LoginResponse(String tokenName, String tokenValue, SessionUser user) {
    }

    public record SessionUser(Long id, String name, Set<String> roles, Set<String> permissions) {
    }
}
