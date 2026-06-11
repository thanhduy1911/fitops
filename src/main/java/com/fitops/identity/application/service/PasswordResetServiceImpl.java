package com.fitops.identity.application.service;

import com.fitops.commons.security.OpaqueTokens;
import com.fitops.commons.security.PasswordResetProperties;
import com.fitops.identity.domain.entity.PasswordResetToken;
import com.fitops.identity.infrastructure.persistence.PasswordResetTokenRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {
  private final PasswordResetTokenRepository repository;
  private final PasswordResetProperties properties;
  private final Clock clock;

  public PasswordResetServiceImpl(
      PasswordResetTokenRepository repository, PasswordResetProperties properties, Clock clock) {
    this.repository = repository;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  @Transactional
  public IssuedResetToken issue(UUID userId) {
    var now = OffsetDateTime.now(clock);
    repository.consumeAllActiveByUserId(userId, now);
    var rawToken = OpaqueTokens.generate();
    var expiresAt = now.plus(properties.ttl());
    repository.save(
        PasswordResetToken.builder()
            .userId(userId)
            .tokenHash(OpaqueTokens.sha256Hex(rawToken))
            .expiresAt(expiresAt)
            .build());

    return new IssuedResetToken(rawToken, expiresAt);
  }

  @Override
  @Transactional
  public Optional<UUID> consume(String rawToken) {
    if (StringUtils.isBlank(rawToken)) {
      return Optional.empty();
    }
    var tokenOpt = repository.findByTokenHash(OpaqueTokens.sha256Hex(rawToken));
    if (tokenOpt.isEmpty()) {
      return Optional.empty();
    }
    var token = tokenOpt.get();
    if (token.getConsumedAt() != null) {
      return Optional.empty();
    }
    var now = OffsetDateTime.now(clock);
    if (!token.getExpiresAt().isAfter(now)) {
      return Optional.empty();
    }
    token.setConsumedAt(now);
    return Optional.of(token.getUserId());
  }
}
