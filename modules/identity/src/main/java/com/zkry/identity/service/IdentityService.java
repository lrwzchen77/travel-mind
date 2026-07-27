package com.zkry.identity.service;

import com.zkry.common.core.exception.BizException;
import com.zkry.common.core.exception.CommonErrorCode;
import com.zkry.identity.domain.IdentityAccount;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class IdentityService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public IdentityService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public IdentityAccount authenticate(String username, String password, String portalRole) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isEmpty() || password == null || password.isEmpty()) {
            throw new BizException("请输入账号和密码。");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT u.id, u.username, u.nickname, u.status, a.password_hash, a.role_code,
                   a.status AS account_status, a.auth_version
            FROM tm_identity_account a
            JOIN tm_user u ON u.id = a.user_id AND u.deleted = 0
            WHERE u.username = :username
            LIMIT 1
            """, Map.of("username", normalizedUsername));
        if (rows.isEmpty()) {
            throw new BizException("账号或密码错误。");
        }
        Map<String, Object> row = rows.get(0);
        IdentityAccount account = map(row);
        if (account.status() != 1 || number(row.get("account_status")) != 1) {
            throw new BizException("账号已停用，请联系管理员。");
        }
        if (!passwordEncoder.matches(password, account.passwordHash())) {
            throw new BizException("账号或密码错误。");
        }
        if ("admin".equals(portalRole) && !"admin".equals(account.roleCode())) {
            throw new BizException("该账号没有管理端访问权限。");
        }
        if ("user".equals(portalRole) && "admin".equals(account.roleCode())) {
            throw new BizException("管理员账号请从管理端登录。");
        }
        return account;
    }

    public IdentityAccount findByUserId(long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT u.id, u.username, u.nickname, u.status, a.password_hash, a.role_code,
                   a.status AS account_status, a.auth_version
            FROM tm_identity_account a
            JOIN tm_user u ON u.id = a.user_id AND u.deleted = 0
            WHERE u.id = :userId
            LIMIT 1
            """, Map.of("userId", userId));
        if (rows.isEmpty()) throw new BizException(CommonErrorCode.UNAUTHORIZED);
        Map<String, Object> row = rows.get(0);
        if (number(row.get("status")) != 1 || number(row.get("account_status")) != 1) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
        }
        return map(row);
    }

    @Transactional
    public Map<String, Object> provision(Map<String, Object> payload) {
        return provision(payload, Set.of("user", "admin"), 10);
    }

    @Transactional
    public void register(String username, String nickname, String password) {
        provision(Map.of(
            "username", username == null ? "" : username,
            "nickname", nickname == null ? "" : nickname,
            "password", password == null ? "" : password,
            "role", "user"
        ), Set.of("user"), 10);
    }

    public boolean hasAdministrator() {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM tm_identity_account
            WHERE role_code = 'admin' AND status = 1
            """, Map.of(), Integer.class);
        return count != null && count > 0;
    }

    @Transactional
    public void changePassword(long userId, String currentPassword, String newPassword) {
        IdentityAccount account = findByUserId(userId);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, account.passwordHash())) {
            throw new BizException("当前密码错误。");
        }
        updatePassword(userId, newPassword);
    }

    @Transactional
    public void resetPassword(long userId, String newPassword) {
        findExistingByUserId(userId);
        updatePassword(userId, newPassword);
    }

    @Transactional
    public void updateRole(long userId, String role) {
        if (!Set.of("user", "admin").contains(role)) throw new BizException("账号角色无效。");
        IdentityAccount current = findExistingByUserId(userId);
        if (current.status() == 1 && "admin".equals(current.roleCode()) && "user".equals(role) && activeAdministratorCount() <= 1) {
            throw new BizException("不能移除最后一个可用管理员。");
        }
        int changed = jdbcTemplate.update("""
            UPDATE tm_identity_account
            SET role_code = :role, auth_version = auth_version + 1
            WHERE user_id = :userId
            """, Map.of("role", role, "userId", userId));
        if (changed == 0) throw new BizException("登录账号不存在。");
    }

    @Transactional
    public void updateStatus(long userId, int status) {
        if (status != 0 && status != 1) throw new BizException("账号状态无效。");
        IdentityAccount current = findExistingByUserId(userId);
        if (status == 0 && "admin".equals(current.roleCode()) && activeAdministratorCount() <= 1) {
            throw new BizException("不能停用最后一个可用管理员。");
        }
        jdbcTemplate.update("UPDATE tm_user SET status = :status WHERE id = :userId AND deleted = 0",
            Map.of("status", status, "userId", userId));
        jdbcTemplate.update("""
            UPDATE tm_identity_account SET status = :status, auth_version = auth_version + 1
            WHERE user_id = :userId
            """, Map.of("status", status, "userId", userId));
    }

    @Transactional
    public void deactivate(long userId) {
        jdbcTemplate.update("UPDATE tm_user SET status = 0 WHERE id = :userId AND deleted = 0", Map.of("userId", userId));
        int changed = jdbcTemplate.update("""
            UPDATE tm_identity_account
            SET status = 0, auth_version = auth_version + 1
            WHERE user_id = :userId
            """, Map.of("userId", userId));
        if (changed == 0) throw new BizException("登录账号不存在。");
    }

    @Transactional
    public Map<String, Object> provisionInitialAdministrator(Map<String, Object> payload) {
        if (hasAdministrator()) return Map.of();
        return provision(payload, Set.of("admin"), 12);
    }

    private Map<String, Object> provision(Map<String, Object> payload, Set<String> allowedRoles, int passwordMinLength) {
        String username = required(payload, "username", 4, 64);
        if (!username.matches("[A-Za-z0-9_.-]+")) throw new BizException("账号只能包含字母、数字、点、横线和下划线。");
        String nickname = required(payload, "nickname", 1, 64);
        String password = required(payload, "password", passwordMinLength, 128);
        String role = text(payload, "role", "user");
        if (!allowedRoles.contains(role)) throw new BizException("账号角色无效。");
        long userId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        try {
            jdbcTemplate.update("""
                INSERT INTO tm_user (id, username, nickname, phone, email, status)
                VALUES (:id, :username, :nickname, :phone, :email, 1)
                """, Map.of(
                "id", userId, "username", username, "nickname", nickname,
                "phone", text(payload, "phone", ""), "email", text(payload, "email", "")));
            jdbcTemplate.update("""
                INSERT INTO tm_identity_account (user_id, password_hash, role_code, status)
                VALUES (:userId, :passwordHash, :role, 1)
                """, Map.of("userId", userId, "passwordHash", passwordEncoder.encode(password), "role", role));
        } catch (DataIntegrityViolationException ex) {
            throw new BizException("账号已存在或资料格式无效。");
        }
        return Map.of("id", userId, "username", username, "nickname", nickname, "role", role, "status", 1);
    }

    private void updatePassword(long userId, String newPassword) {
        String password = newPassword == null ? "" : newPassword;
        if (password.length() < 10 || password.length() > 128) throw new BizException("新密码长度应为 10 到 128 位。");
        jdbcTemplate.update("""
            UPDATE tm_identity_account
            SET password_hash = :passwordHash, auth_version = auth_version + 1
            WHERE user_id = :userId
            """, Map.of("passwordHash", passwordEncoder.encode(password), "userId", userId));
    }

    private int activeAdministratorCount() {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM tm_identity_account a
            JOIN tm_user u ON u.id = a.user_id AND u.deleted = 0 AND u.status = 1
            WHERE a.role_code = 'admin' AND a.status = 1
            """, Map.of(), Integer.class);
        return count == null ? 0 : count;
    }

    private IdentityAccount findExistingByUserId(long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT u.id, u.username, u.nickname, u.status, a.password_hash, a.role_code,
                   a.status AS account_status, a.auth_version
            FROM tm_identity_account a JOIN tm_user u ON u.id = a.user_id AND u.deleted = 0
            WHERE u.id = :userId LIMIT 1
            """, Map.of("userId", userId));
        if (rows.isEmpty()) throw new BizException("登录账号不存在。");
        return map(rows.get(0));
    }

    private IdentityAccount map(Map<String, Object> row) {
        return new IdentityAccount(
            number(row.get("id")),
            String.valueOf(row.get("username")),
            String.valueOf(row.getOrDefault("nickname", row.get("username"))),
            String.valueOf(row.get("password_hash")),
            String.valueOf(row.getOrDefault("role_code", "user")),
            number(row.get("status")).intValue(),
            number(row.getOrDefault("auth_version", 0L))
        );
    }

    private Long number(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    private String required(Map<String, Object> payload, String key, int min, int max) {
        String value = text(payload, key, "").trim();
        if (value.length() < min || value.length() > max) throw new BizException(key + " 长度无效。");
        return value;
    }

    private String text(Map<String, Object> payload, String key, String fallback) {
        Object value = payload == null ? null : payload.get(key);
        String result = value == null ? fallback : String.valueOf(value).trim();
        if (result.length() > 255) throw new BizException(key + " 内容过长。");
        return result;
    }

}
