package com.zkry.config;

import cn.dev33.satoken.stp.StpUtil;
import com.zkry.common.core.exception.BizException;
import com.zkry.common.core.exception.CommonErrorCode;
import com.zkry.common.satoken.core.LoginHelper;
import com.zkry.common.satoken.core.LoginUser;
import com.zkry.identity.domain.IdentityAccount;
import com.zkry.identity.service.IdentityService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AccountStateInterceptor implements HandlerInterceptor {

    private final IdentityService identityService;

    public AccountStateInterceptor(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getDispatcherType() == DispatcherType.ASYNC || "OPTIONS".equalsIgnoreCase(request.getMethod())
            || !StpUtil.isLogin()) return true;
        LoginUser loginUser = LoginHelper.getLoginUser();
        IdentityAccount account = identityService.findByUserId(loginUser.userId());
        if (loginUser.authVersion() != account.authVersion() || !loginUser.roles().equals(account.roles())) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
        }
        return true;
    }
}
