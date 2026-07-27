package com.zkry.common.satoken.core;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 存储在 Sa-Token JWT 中的最小登录用户快照。
 *
 * <p>这个 record 不是数据库实体，也不应该和用户表一一绑定。
 * 它只保存“每次请求经常需要用到”的认证上下文信息，例如用户 ID、用户名、角色、权限。
 *
 * <p>为什么只保存快照：
 *
 * <p>1. 认证模块不需要依赖 identity/user 模块里的 domain，模块边界更干净。
 *
 * <p>2. 避免把密码、盐值、手机号、邮箱等敏感字段放入 JWT。
 *
 * <p>3. 业务鉴权直接读取 JWT；请求入口只查询最小账号状态以支持即时失效。
 *
 * <p>用户角色、状态或密码发生变化时，authVersion 会让旧 JWT 立即失效。
 */
public record LoginUser(
    /**
     * 用户主键 ID。
     *
     * <p>这里通常对应用户表主键，也是 Sa-Token 的 loginId。
     */
    Long userId,

    /**
     * 用户名或展示名。
     *
     * <p>用于日志、审计、页面展示等轻量场景，不建议放入手机号、邮箱等敏感标识。
     */
    String username,

    /**
     * 当前用户拥有的角色编码。
     *
     * <p>例如 admin、user。角色是粗粒度身份，适合判断“是否管理员”等场景。
     */
    Set<String> roles,

    /**
     * 当前用户拥有的权限编码。
     *
     * <p>例如 user:list、credit:adjust。权限是细粒度能力，适合控制具体菜单、按钮和接口。
     */
    Set<String> permissions,

    /** 账号凭据版本；密码、角色或状态变化后旧 JWT 立即失效。 */
    long authVersion
) implements Serializable {

    /**
     * 序列化版本号。
     *
     * <p>保留 serialVersionUID，避免作为通用认证快照传递时出现序列化兼容问题。
     */
    @Serial
    private static final long serialVersionUID = 1L;
}
