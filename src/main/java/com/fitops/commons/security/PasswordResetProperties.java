package com.fitops.commons.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "fitops.security.password-reset")
public record PasswordResetProperties(@NotNull Duration ttl, @NotBlank String resetUrlBase) {
  public PasswordResetProperties {
    if (ttl == null || ttl.compareTo(Duration.ZERO) <= 0) {
      throw new IllegalArgumentException(
          "fitops.security.password-reset.ttl must be a positive duration (got: " + ttl + ")");
    }
  }
}
