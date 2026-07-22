package com.fitops.identity.application.port;

import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/**
 * Minimal cross-module projection of a user, published to consumer modules.
 *
 * <p>Carries no PII: {@code email}, password, roles and every other sensitive field are
 * deliberately absent, so even a boundary mistake cannot leak sensitive identity data.
 *
 * @param displayName may be {@code null}, consumers fall back to {@code username}.
 */
@NamedInterface("port")
public record UserSummary(UUID id, String username, String displayName, String language) {}
