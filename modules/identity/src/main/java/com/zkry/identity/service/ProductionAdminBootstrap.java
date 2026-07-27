package com.zkry.identity.service;

import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionAdminBootstrap implements ApplicationRunner {

    private final IdentityService identityService;
    private final Environment environment;

    public ProductionAdminBootstrap(IdentityService identityService, Environment environment) {
        this.identityService = identityService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (identityService.hasAdministrator()) return;
        String username = required("TRAVELMIND_BOOTSTRAP_ADMIN_USERNAME");
        String password = required("TRAVELMIND_BOOTSTRAP_ADMIN_PASSWORD");
        String nickname = environment.getProperty("TRAVELMIND_BOOTSTRAP_ADMIN_NICKNAME", "系统管理员");
        identityService.provisionInitialAdministrator(Map.of(
            "username", username,
            "nickname", nickname,
            "password", password,
            "role", "admin"
        ));
    }

    private String required(String key) {
        String value = environment.getProperty(key, "").trim();
        if (value.isEmpty()) throw new IllegalStateException("生产环境首次启动缺少 " + key);
        return value;
    }
}
