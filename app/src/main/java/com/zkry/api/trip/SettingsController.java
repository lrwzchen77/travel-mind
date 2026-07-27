package com.zkry.api.trip;

import com.zkry.common.core.config.TravelMindRuntimeSettingsService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/settings")
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    private final TravelMindRuntimeSettingsService runtimeSettingsService;
    private final Environment environment;

    public SettingsController(TravelMindRuntimeSettingsService runtimeSettingsService, Environment environment) {
        this.runtimeSettingsService = runtimeSettingsService;
        this.environment = environment;
    }

    @GetMapping
    public Map<String, Object> get() {
        log.info("[Settings] 读取运行时配置 snapshotKeys={}", runtimeSettingsService.snapshot().keySet());
        return response("配置读取成功");
    }

    @PutMapping
    public Map<String, Object> save(@RequestBody Map<String, Object> updates) {
        if (environment.matchesProfiles("prod")) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "生产环境配置由密钥与环境变量管理");
        }
        Set<String> keys = updates == null ? Set.of() : updates.keySet();
        // 只记录被更新的配置项名称，避免把 Cookie、API Key 等敏感值打印到控制台。
        log.info("[Settings] 保存运行时配置 updateKeys={}", keys);
        runtimeSettingsService.update(updates);
        return response("配置已保存到 Java 运行时内存");
    }

    private Map<String, Object> response(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", message);
        body.put("mutable", !environment.matchesProfiles("prod"));
        body.put("data", runtimeSettingsService.publicSnapshot());
        return body;
    }
}
