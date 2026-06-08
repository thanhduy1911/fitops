package com.fitops.identity.application.service;

import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitops.commons.security.RefreshTokenProperties;
import com.fitops.identity.domain.entity.RefreshToken;
import com.fitops.identity.infrastructure.persistence.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private RefreshTokenProperties refreshTokenProperties;

  @Test
  @DisplayName(
      "issue() persists only the SHA-256 hash, returns the raw token, ttl applied, revoked=false")
  void issue_persistsHash_returnsRaw() throws Exception {
    when(refreshTokenProperties.ttl()).thenReturn(Duration.ofDays(7));
    var service = new RefreshTokenServiceImpl(refreshTokenRepository, refreshTokenProperties);
    var userId = UUID.randomUUID();

    String rawToken = service.issue(userId);

    var captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(captor.capture());
    var saved = captor.getValue();

    assertThat(rawToken).isNotBlank();
    assertThat(rawToken).matches("[A-Za-z0-9_-]+");
    assertThat(rawToken).hasSize(43);

    assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
    assertThat(saved.getTokenHash()).isEqualTo(sha256Hex(rawToken));

    assertThat(saved.getUserId()).isEqualTo(userId);
    assertThat(saved.isRevoked()).isFalse();
    assertThat(saved.getExpiresAt())
        .isCloseTo(OffsetDateTime.now(ZoneOffset.UTC).plusDays(7), within(5, ChronoUnit.SECONDS));
  }

  private static String sha256Hex(String value) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
