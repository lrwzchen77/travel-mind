package com.zkry.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.dev33.satoken.jwt.SaJwtUtil;
import cn.dev33.satoken.spring.SaBeanInject;
import cn.dev33.satoken.spring.SaBeanRegister;
import cn.dev33.satoken.spring.SaTokenContextRegister;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkry.common.redis.util.RedisUtils;
import com.zkry.common.satoken.config.SaTokenConfig;
import com.zkry.common.satoken.core.SaPermissionService;
import com.zkry.common.satoken.exception.SaTokenExceptionHandler;
import com.zkry.identity.domain.IdentityAccount;
import com.zkry.identity.service.IdentityService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({
    SaBeanRegister.class,
    SaBeanInject.class,
    SaTokenContextRegister.class,
    SaTokenConfig.class,
    SaPermissionService.class,
    SaTokenExceptionHandler.class
})
@TestPropertySource(properties = {
    "sa-token.jwt-secret-key=jwt-test-secret-with-at-least-32-bytes",
    "sa-token.timeout=3600"
})
class AuthControllerJwtTest {

    private static final String SECRET = "jwt-test-secret-with-at-least-32-bytes";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IdentityService identityService;

    @MockitoBean
    private RedisUtils redisUtils;

    @Test
    void loginIssuesSignedJwtWithIdentityRoleAndExpiry() throws Exception {
        String token = login("user", account(1001L, "demo", "旅行者", "user"));

        assertThat(token.split("\\.")).hasSize(3);
        var payload = SaJwtUtil.getPayloads(token, "login", SECRET);
        assertThat(payload.getLong("loginId")).isEqualTo(1001L);
        assertThat(payload.getStr("name")).isEqualTo("旅行者");
        assertThat(payload.getStr("roles")).isEqualTo("user");
        assertThat(payload.getLong("exp")).isGreaterThan(System.currentTimeMillis() / 1000);
        assertThat(payload.getLong("eff")).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    void protectedEndpointsAcceptMatchingRoleAndRejectWrongPortal() throws Exception {
        String userToken = login("user", account(1001L, "demo", "旅行者", "user"));
        String adminToken = login("admin", account(1L, "admin", "管理员", "admin"));

        mvc.perform(get("/api/user/auth/me").header("Authorization", userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1001))
            .andExpect(jsonPath("$.data.roles[0]").value("user"));
        mvc.perform(get("/api/admin/auth/me").header("Authorization", adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roles[0]").value("admin"));
        mvc.perform(get("/api/admin/auth/me").header("Authorization", userToken))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/user/auth/me").header("Authorization", adminToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void tamperedAndExpiredTokensAreRejected() throws Exception {
        String token = login("user", account(1001L, "demo", "旅行者", "user"));
        char replacement = token.endsWith("a") ? 'b' : 'a';
        String tampered = token.substring(0, token.length() - 1) + replacement;
        String expired = SaJwtUtil.createToken(
            "login", 1001L, "default-device", 0,
            Map.of("name", "旅行者", "roles", "user", "permissions", "trip:manage", "exp", 0),
            SECRET
        );
        Thread.sleep(5);

        mvc.perform(get("/api/user/auth/me").header("Authorization", tampered))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/user/auth/me").header("Authorization", expired))
            .andExpect(status().isUnauthorized());
    }

    private String login(String portal, IdentityAccount account) throws Exception {
        when(identityService.authenticate(anyString(), anyString(), anyString())).thenReturn(account);
        String body = mvc.perform(post("/api/{portal}/auth/login", portal)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + account.username() + "\",\"password\":\"secret\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tokenName").value("Authorization"))
            .andReturn().getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(body);
        return response.path("data").path("tokenValue").asText();
    }

    private IdentityAccount account(long id, String username, String nickname, String role) {
        return new IdentityAccount(id, username, nickname, "hash", role, 1);
    }
}
