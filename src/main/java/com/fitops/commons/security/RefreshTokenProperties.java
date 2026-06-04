package com.fitops.commons.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "fitops.security.refresh-token")
public record RefreshTokenProperties(
    @NotNull Duration ttl,
    @NotBlank String cookieName,
    @NotBlank String cookiePath,
    boolean secure) {}
