package com.fitops.commons.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fitops.security.password")
public record PasswordProperties(@Min(10) @Max(14) int bcryptStrength) {}
