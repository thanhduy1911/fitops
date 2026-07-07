package com.fitops.identity.api;

import com.fitops.identity.config.RefreshTokenProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookies {
  private static final String SAME_SITE_STRICT = "Strict";

  private final RefreshTokenProperties properties;

  public RefreshTokenCookies(RefreshTokenProperties properties) {
    this.properties = properties;
  }

  /** Issues the refresh cookie carrying the opaque token, valid for the configured TTL. */
  public ResponseCookie issue(String rawToken) {
    return build(rawToken, properties.ttl());
  }

  /** Clears the refresh cookie on logout: empty value, maxAge = 0. */
  public ResponseCookie clear() {
    return build("", Duration.ZERO);
  }

  private ResponseCookie build(String value, Duration maxAge) {
    return ResponseCookie.from(properties.cookieName(), value)
        .httpOnly(true)
        .secure(properties.secure())
        .sameSite(SAME_SITE_STRICT)
        .path(properties.cookiePath())
        .maxAge(maxAge)
        .build();
  }
}
