package com.zkry.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.zkry.common.redis.util.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RequestRateLimitInterceptorTest {

    @Test
    void rejectsLoginAfterMinuteLimit() {
        RedisUtils redis = org.mockito.Mockito.mock(RedisUtils.class);
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/user/auth/login");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(redis.increment(org.mockito.ArgumentMatchers.anyString())).thenReturn(11L);
        RequestRateLimitInterceptor interceptor = new RequestRateLimitInterceptor(redis);
        assertThatThrownBy(() -> interceptor.preHandle(request, org.mockito.Mockito.mock(HttpServletResponse.class), new Object()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("429");
    }
}
