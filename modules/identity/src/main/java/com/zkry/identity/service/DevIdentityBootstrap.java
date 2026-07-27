package com.zkry.identity.service;

import java.util.Map;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevIdentityBootstrap implements ApplicationListener<ApplicationReadyEvent> {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DevIdentityBootstrap(NamedParameterJdbcTemplate jdbcTemplate, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        createUser(1001L, "demo_user", "旅行体验用户", environment.getProperty("TRAVELMIND_DEMO_PASSWORD", "travel123"), "user");
        createUser(9001L, "admin", "运营管理员", environment.getProperty("TRAVELMIND_ADMIN_PASSWORD", "admin123"), "admin");
    }

    private void createUser(long userId, String username, String nickname, String password, String role) {
        jdbcTemplate.update("""
            INSERT INTO tm_user (id, username, nickname, status)
            VALUES (:id, :username, :nickname, 1)
            ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), update_time = CURRENT_TIMESTAMP
            """, Map.of("id", userId, "username", username, "nickname", nickname));
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tm_identity_account WHERE user_id = :userId",
            Map.of("userId", userId), Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.update("""
                INSERT INTO tm_identity_account (user_id, password_hash, role_code, status)
                VALUES (:userId, :passwordHash, :roleCode, 1)
                """, Map.of(
                    "userId", userId,
                    "passwordHash", passwordEncoder.encode(password),
                    "roleCode", role
                ));
        }
    }
}
