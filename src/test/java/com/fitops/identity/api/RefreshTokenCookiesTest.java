package com.fitops.identity.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.identity.config.RefreshTokenProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookiesTest {
  private static final RefreshTokenProperties PROPERTIES =
      new RefreshTokenProperties(Duration.ofDays(7), "refreshToken", "/api/v1/auth", true);

  private final RefreshTokenCookies cookies = new RefreshTokenCookies(PROPERTIES);

  @Test
  void issue_carriesTokenWithFixedPolicyAndTtl() {
    ResponseCookie cookie = cookies.issue("raw-token");

    assertThat(cookie.getName()).isEqualTo("refreshToken");
    assertThat(cookie.getValue()).isEqualTo("raw-token");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.getSameSite()).isEqualTo("Strict");
    assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(7));
  }

  @Test
  void clear_emptiesValueAndExpiresImmediately_keepingFixedPolicy() {
    ResponseCookie cookie = cookies.clear();

    assertThat(cookie.getName()).isEqualTo("refreshToken");
    assertThat(cookie.getValue()).isEmpty();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.getSameSite()).isEqualTo("Strict");
    assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
  }
}
