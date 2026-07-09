package com.fitops.identity.api.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String username,
    String displayName,
    String avatarUrl,
    String language,
    OffsetDateTime createdAt) {}
