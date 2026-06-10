package com.fitops.identity.application.service;

import java.time.OffsetDateTime;

public record IssuedResetToken(String rawToken, OffsetDateTime expiresAt) {}
