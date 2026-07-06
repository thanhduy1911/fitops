package com.fitops.identity.application.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fitops.identity.config.RefreshTokenProperties;
import com.fitops.identity.domain.entity.RefreshToken;
import com.fitops.identity.infrastructure.persistence.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.Optional;
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
  private final Clock clock = Clock.fixed(Instant.parse("2026-06-10T12:00:00Z"), ZoneOffset.UTC);

  @Test
  @DisplayName(
      "issue() persists only the SHA-256 hash, returns the raw token, ttl applied, revoked=false")
  void issue_persistsHash_returnsRaw() throws Exception {
    when(refreshTokenProperties.ttl()).thenReturn(Duration.ofDays(7));
    var service =
        new RefreshTokenServiceImpl(refreshTokenRepository, refreshTokenProperties, clock);
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
    assertThat(saved.getExpiresAt()).isEqualTo(OffsetDateTime.now(clock).plusDays(7));
  }

  @Test
  @DisplayName("rotate() valid token -> marks old revoked, returns userId")
  void rotate_valid_revokesOld_returnsUserId() {
    var service =
        new RefreshTokenServiceImpl(refreshTokenRepository, refreshTokenProperties, clock);
    var userId = UUID.randomUUID();
    var token =
        RefreshToken.builder()
            .userId(userId)
            .tokenHash("hash")
            .expiresAt(OffsetDateTime.now(clock).plusDays(7))
            .build();
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    var result = service.rotate("raw-token");

    assertThat(result.isPresent()).isTrue();
    //noinspection OptionalGetWithoutIsPresent
    assertThat(result.get()).isEqualTo(userId);
    assertThat(token.isRevoked()).isTrue();
    verify(refreshTokenRepository, never()).revokeAllActiveByUserId(any());
  }

  @Test
  @DisplayName("rotate() reuse of a revoked token -> revokes the whole family, returns empty")
  void rotate_reuseRevoked_revokesAll_returnsEmpty() {
    var service =
        new RefreshTokenServiceImpl(refreshTokenRepository, refreshTokenProperties, clock);
    var userId = UUID.randomUUID();
    var revoked =
        RefreshToken.builder()
            .userId(userId)
            .tokenHash("hash")
            .expiresAt(OffsetDateTime.now(clock).plusDays(7))
            .revoked(true)
            .build();
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

    var result = service.rotate("raw-token");

    assertThat(result.isEmpty()).isTrue();
    verify(refreshTokenRepository).revokeAllActiveByUserId(userId);
  }

  @Test
  @DisplayName("rotate() expired token -> returns empty, no rotation, no family revoke")
  void rotate_expired_returnsEmpty() {
    var service =
        new RefreshTokenServiceImpl(refreshTokenRepository, refreshTokenProperties, clock);
    var token =
        RefreshToken.builder()
            .userId(UUID.randomUUID())
            .tokenHash("hash")
            .expiresAt(OffsetDateTime.now(clock).minusSeconds(1))
            .build();
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    var result = service.rotate("raw-token");

    assertThat(result.isEmpty()).isTrue();
    assertThat(token.isRevoked()).isFalse();
    verify(refreshTokenRepository, never()).revokeAllActiveByUserId(any());
  }

  @Test
  @DisplayName("rotate() unknown hash -> empty")
  void rotate_unknown_returnsEmpty() {
    var service =
        new RefreshTokenServiceImpl(refreshTokenRepository, refreshTokenProperties, clock);
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertThat(service.rotate("raw-token").isEmpty()).isTrue();
  }

  @Test
  @DisplayName("rotate() blank input -> empty, never touches the repository")
  void rotate_blank_returnsEmpty() {
    var service =
        new RefreshTokenServiceImpl(refreshTokenRepository, refreshTokenProperties, clock);

    assertThat(service.rotate("   ").isEmpty()).isTrue();
    verifyNoInteractions(refreshTokenRepository);
  }

  @Test
  @DisplayName("revoke() present token -> sets revoked=true")
  void revoke_present_setsRevoked() {
    var service =
        new RefreshTokenServiceImpl(refreshTokenRepository, refreshTokenProperties, clock);
    var token =
        RefreshToken.builder()
            .userId(UUID.randomUUID())
            .tokenHash("hash")
            .expiresAt(OffsetDateTime.now(clock).plusDays(7))
            .build();
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    service.revoke("raw-token");

    assertThat(token.isRevoked()).isTrue();
  }

  @Test
  @DisplayName("revoke() unknown token -> no-op, no exception")
  void revoke_unknown_noop() {
    var service =
        new RefreshTokenServiceImpl(refreshTokenRepository, refreshTokenProperties, clock);
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    service.revoke("raw-token"); // must not throw
  }

  private static String sha256Hex(String value) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
