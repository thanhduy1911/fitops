package com.fitops.identity.application.port;

import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/**
 * Minimal cross-module projection of a user, published to consumer modules.
 *
 * <p>Excludes credentials and contact data: the password hash, {@code email} and role assignments
 * are deliberately absent, so a boundary mistake cannot leak them. What remains is still personal
 * data. {@code id}, {@code username} and {@code displayName} identify a user, and {@code
 * displayName} is often a real name. Treat this record as personal data when logging or forwarding
 * it, and keep new fields to the same bar: no credentials, no contact details.
 *
 * @param displayName may be {@code null}, consumers fall back to {@code username}.
 */
@NamedInterface("port")
public record UserSummary(UUID id, String username, String displayName, String language) {}
