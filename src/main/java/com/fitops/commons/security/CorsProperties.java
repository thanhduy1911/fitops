package com.fitops.commons.security;

import jakarta.validation.constraints.NotEmpty;

import java.time.Duration;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fitops.security.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins, @NotNull Duration maxAge) {}
