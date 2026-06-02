package com.fitops.commons.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fitops.cache")
public record CacheProperties(@NotNull Duration defaultTtl, @Positive long defaultMaxSize) {}
