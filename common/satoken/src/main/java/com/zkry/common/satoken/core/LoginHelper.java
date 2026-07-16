package com.zkry.common.satoken.core;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录态工具类。
 *
 * <p>这个类是对 Sa-Token {@link StpUtil} 的轻量封装，目的不是隐藏 Sa-Token，
 * 而是给业务模块提供一组稳定、简单、符合项目约定的登录态 API。
 *
 * <p>为什么不建议业务代码到处直接调用 StpUtil：
 *
 * <p>1. 登录态里保存哪些字段，需要项目统一约定，否则后面管理端、用户端会各写一套。
 *
 * <p>2. 后续如果切换登录 ID 类型、增加租户 ID、增加账号类型，统一封装会更容易改。
 *
 * <p>3. Controller/Service 中调用 LoginHelper，可读性比散落一堆 StpUtil 细节更好。
 *
 * <p>注意：这个类只维护认证上下文，不直接查询用户表，也不负责账号密码校验。
 * 用户是否存在、密码是否正确、账号是否禁用，应该由 identity/user 等业务模块处理。
 */
public final class LoginHelper {

    /** JWT 只携带最小认证快照，不放密码、手机号等敏感字段。 */
    private static final String NAME = "name";
    private static final String ROLES = "roles";
    private static final String PERMISSIONS = "permissions";

    /**
     * 工具类不允许实例化。
     */
    private LoginHelper() {
    }

    /**
     * 使用用户 ID 作为登录标识，并把最小登录用户快照写入 JWT。
     *
     * <p>Sa-Token 的 loginId 只需要一个唯一标识，这里选用 userId。
     * 角色、权限、用户名等展示和鉴权辅助信息放在 JWT claims，避免每次请求都查数据库。
     *
     * @param loginUser 当前登录用户快照
     */
    public static void login(LoginUser loginUser) {
        long timeout = StpUtil.getStpLogic().getConfigOrGlobal().getTimeout();
        SaLoginParameter parameter = SaLoginParameter.create()
            .setExtra(NAME, loginUser.username())
            .setExtra(ROLES, String.join(",", loginUser.roles()))
            .setExtra(PERMISSIONS, String.join(",", loginUser.permissions()))
            .setExtra("exp", Instant.now().getEpochSecond() + timeout);
        StpUtil.login(loginUser.userId(), parameter);
    }

    /**
     * 当前请求退出登录。无状态 JWT 不做服务端吊销，客户端必须同时丢弃 token。
     *
     * <p>普通用户端“退出登录”按钮、管理端退出登录都可以调用这个方法。
     */
    public static void logout() {
        StpUtil.logout();
    }

    /**
     * 判断当前请求是否已登录。
     *
     * <p>适合在可匿名访问的接口里做分支逻辑，例如“未登录也能看详情，登录后额外返回是否收藏”。
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * 获取当前登录用户 ID。
     *
     * <p>如果当前请求未登录，Sa-Token 会抛出未登录异常。
     * 业务代码调用这个方法前，一般应该确保当前接口已经经过登录拦截。
     */
    public static Long getUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 未登录时返回 null；有效 JWT 直接还原最小用户快照。
     */
    public static LoginUser getLoginUser() {
        if (!StpUtil.isLogin()) return null;
        return new LoginUser(
            getUserId(),
            String.valueOf(StpUtil.getExtra(NAME)),
            claimSet(ROLES),
            claimSet(PERMISSIONS)
        );
    }

    private static Set<String> claimSet(String name) {
        Object value = StpUtil.getExtra(name);
        if (value == null || value.toString().isBlank()) return Set.of();
        return Arrays.stream(value.toString().split(","))
            .filter(item -> !item.isBlank())
            .collect(Collectors.toUnmodifiableSet());
    }
}
