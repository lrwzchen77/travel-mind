package com.zkry.common.satoken.core;

import cn.dev33.satoken.stp.StpInterface;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SaPermissionService implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        LoginUser loginUser = LoginHelper.getLoginUser();
        return loginUser == null ? List.of() : new ArrayList<>(loginUser.permissions());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        LoginUser loginUser = LoginHelper.getLoginUser();
        return loginUser == null ? List.of() : new ArrayList<>(loginUser.roles());
    }
}
