package com.zkry.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zkry.common.core.exception.BizException;
import com.zkry.identity.domain.IdentityAccount;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class IdentityServiceTest {

    @Test
    void authenticatesUserWithBcryptHash() {
        NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Map.of("username", "demo_user"))))
            .thenReturn(List.of(account("demo_user", "user", "travel123")));

        IdentityAccount account = new IdentityService(jdbcTemplate).authenticate("demo_user", "travel123", "user");

        assertThat(account.userId()).isEqualTo(1001L);
        assertThat(account.roles()).containsExactly("user");
    }

    @Test
    void rejectsWrongPasswordAndAdminPortalRoleMismatch() {
        NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(Map.of("username", "demo_user"))))
            .thenReturn(List.of(account("demo_user", "user", "travel123")));

        IdentityService service = new IdentityService(jdbcTemplate);

        assertThatThrownBy(() -> service.authenticate("demo_user", "wrong", "user"))
            .isInstanceOf(BizException.class).hasMessage("账号或密码错误。");
        assertThatThrownBy(() -> service.authenticate("demo_user", "travel123", "admin"))
            .isInstanceOf(BizException.class).hasMessage("该账号没有管理端访问权限。");
    }

    @Test
    void registrationCreatesOnlyAConsumerWithBcryptPassword() {
        NamedParameterJdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
        IdentityService service = new IdentityService(jdbcTemplate);

        service.register("traveler", "旅行者", "secure-password");

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbcTemplate, times(2)).update(anyString(), params.capture());
        Map<?, ?> identity = params.getAllValues().get(1);
        assertThat(identity.get("role")).isEqualTo("user");
        assertThat(new BCryptPasswordEncoder().matches("secure-password", String.valueOf(identity.get("passwordHash"))))
            .isTrue();
    }

    private Map<String, Object> account(String username, String role, String password) {
        return Map.of(
            "id", 1001L,
            "username", username,
            "nickname", "旅行用户",
            "status", 1,
            "account_status", 1,
            "password_hash", new BCryptPasswordEncoder().encode(password),
            "role_code", role
        );
    }
}
