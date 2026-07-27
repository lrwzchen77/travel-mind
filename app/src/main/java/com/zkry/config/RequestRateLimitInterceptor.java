package com.zkry.config;

import cn.dev33.satoken.stp.StpUtil;
import com.zkry.common.redis.util.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestRateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestRateLimitInterceptor.class);
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private final RedisUtils redis;

    public RequestRateLimitInterceptor(RedisUtils redis) {
        this.redis = redis;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String bucket = path.endsWith("/auth/login") ? "login" : path.startsWith("/api/poi/") ? "poi" : "ai";
        long limit = "login".equals(bucket) ? 10 : "poi".equals(bucket) ? 30 : 60;
        String subject = subject(request);
        String key = "travelmind:rate:" + bucket + ":" + subject;
        try {
            long count = redis.increment(key);
            if (count == 1) redis.getStringRedisTemplate().expire(key, WINDOW);
            if (count > limit) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后重试");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.debug("Rate limiter unavailable; request allowed path={} reason={}", path, ex.getMessage());
        }
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",", 2)[0].trim();
        return request.getRemoteAddr();
    }

    private String subject(HttpServletRequest request) {
        try {
            if (StpUtil.isLogin()) return "user:" + StpUtil.getLoginId();
        } catch (RuntimeException ignored) {
        }
        return "ip:" + clientIp(request);
    }
}
