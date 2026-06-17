package com.fitops.commons.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fitops.security.rate-limit")
public record RateLimitProperties(boolean enabled, int capacity, Duration refillPeriod) {
  public RateLimitProperties {
    if (capacity <= 0) {
      throw new IllegalArgumentException(
          "fitops.security.rate-limit.capacity must be positive (got: " + capacity + ")");
    }
    if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
      throw new IllegalArgumentException(
          "fitops.security.rate-limit.refillPeriod must be a positive duration (got: "
              + refillPeriod
              + ")");
    }
  }
}
