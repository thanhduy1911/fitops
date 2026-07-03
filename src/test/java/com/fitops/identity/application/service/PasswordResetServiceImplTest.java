package com.fitops.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fitops.commons.security.OpaqueTokens;
import com.fitops.identity.config.PasswordResetProperties;
import com.fitops.identity.domain.entity.PasswordResetToken;
import com.fitops.identity.infrastructure.persistence.PasswordResetTokenRepository;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {
  private static final Instant NOW = Instant.parse("2026-06-10T12:00:00.00Z");
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final PasswordResetProperties props =
      new PasswordResetProperties(Duration.ofHours(1), "http://localhost:5173/reset-password");

  @Mock private PasswordResetTokenRepository repository;

  private PasswordResetServiceImpl service() {
    return new PasswordResetServiceImpl(repository, props, clock);
  }

  @Test
  @DisplayName(
      "issue() invalidates prior active tokens, persists only the hash, returns raw = 1h expiry")
  void issue_invalidates_persistsHash_returnsRaw() {
    var userId = UUID.randomUUID();

    var issued = service().issue(userId);

    var nowOdt = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
    verify(repository).consumeAllActiveByUserId(userId, nowOdt);

    var captor = ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(repository).save(captor.capture());
    var saved = captor.getValue();

    assertThat(issued.rawToken()).matches("[A-Za-z0-9_-]+").hasSize(43);
    assertThat(saved.getTokenHash())
        .isEqualTo(OpaqueTokens.sha256Hex(issued.rawToken()))
        .isNotEqualTo(issued.rawToken());
    assertThat(saved.getUserId()).isEqualTo(userId);
    assertThat(saved.getExpiresAt()).isEqualTo(nowOdt.plusHours(1));
    assertThat(issued.expiresAt()).isEqualTo(nowOdt.plusHours(1));
  }

  @Test
  @DisplayName("consume() valid token -> sets consumed_at, returns userId")
  void consume_valid_returnsUserId() {
    var userId = UUID.randomUUID();
    var token =
        PasswordResetToken.builder()
            .userId(userId)
            .tokenHash(OpaqueTokens.sha256Hex("raw"))
            .expiresAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).plusMinutes(30))
            .build();
    when(repository.findByTokenHash(OpaqueTokens.sha256Hex("raw"))).thenReturn(Optional.of(token));

    var result = service().consume("raw");

    assertThat(result).contains(userId);
    assertThat(token.getConsumedAt()).isEqualTo(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("consume() expired token -> empty, consumed_at untouched")
  void consume_expired_returnsEmpty() {
    var token =
        PasswordResetToken.builder()
            .userId(UUID.randomUUID())
            .tokenHash(OpaqueTokens.sha256Hex("raw"))
            .expiresAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).minusSeconds(1))
            .build();
    when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

    assertThat(service().consume("raw")).isEmpty();
    assertThat(token.getConsumedAt()).isNull();
  }

  @Test
  @DisplayName("consume() already-consumed token -> empty")
  void consume_alreadyConsumed_returnsEmpty() {
    var token =
        PasswordResetToken.builder()
            .userId(UUID.randomUUID())
            .tokenHash(OpaqueTokens.sha256Hex("raw"))
            .expiresAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).plusMinutes(30))
            .consumedAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC).minusMinutes(5))
            .build();
    when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

    assertThat(service().consume("raw")).isEmpty();
  }

  @Test
  @DisplayName("consume() unknown hash -> empty")
  void consume_unknown_returnsEmpty() {
    when(repository.findByTokenHash(any())).thenReturn(Optional.empty());
    assertThat(service().consume("raw")).isEmpty();
  }

  @Test
  @DisplayName("consume() blank -> empty, repository never touched")
  void consume_blank_returnsEmpty() {
    assertThat(service().consume("  ")).isEmpty();
    verifyNoInteractions(repository);
  }
}
