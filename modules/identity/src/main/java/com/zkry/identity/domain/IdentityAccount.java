package com.zkry.identity.domain;

import java.util.Set;

public record IdentityAccount(
    Long userId,
    String username,
    String nickname,
    String passwordHash,
    String roleCode,
    int status
) {

    public Set<String> roles() {
        return Set.of(roleCode);
    }

    public Set<String> permissions() {
        return switch (roleCode) {
            case "admin" -> Set.of("admin:access", "resource:manage", "user:manage", "settings:manage");
            case "operator" -> Set.of("admin:access", "resource:manage");
            default -> Set.of("trip:manage", "profile:manage", "favorite:manage");
        };
    }
}
