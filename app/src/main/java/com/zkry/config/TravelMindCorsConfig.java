package com.zkry.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.util.Arrays;

@Configuration
public class TravelMindCorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;
    private final RequestRateLimitInterceptor rateLimit;
    private final AccountStateInterceptor accountState;

    public TravelMindCorsConfig(
        @Value("${travelmind.web.allowed-origins}") String allowedOrigins,
        RequestRateLimitInterceptor rateLimit,
        AccountStateInterceptor accountState
    ) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toArray(String[]::new);
        this.rateLimit = rateLimit;
        this.accountState = accountState;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimit).addPathPatterns(
            "/api/user/auth/login", "/api/user/auth/register", "/api/admin/auth/login",
            "/api/user/ai/**", "/api/admin/ai/**", "/api/user/trip/plan",
            "/api/user/assistant/**", "/api/user/uploads/**", "/api/poi/**");
        registry.addInterceptor(accountState)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/user/auth/login", "/api/user/auth/register", "/api/admin/auth/login",
                "/api/public/**", "/api/poi/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(false)
            .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/public-uploads/**")
            .addResourceLocations(Path.of(System.getProperty("user.dir"), "uploads", "public").toUri().toString());
    }
}
