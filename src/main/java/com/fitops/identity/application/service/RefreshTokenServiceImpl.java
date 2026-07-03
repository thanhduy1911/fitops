package com.fitops.identity.application.service;

import static com.fitops.commons.security.OpaqueTokens.sha256Hex;

import com.fitops.commons.security.OpaqueTokens;
import com.fitops.identity.config.RefreshTokenProperties;
import com.fitops.identity.domain.entity.RefreshToken;
import com.fitops.identity.infrastructure.persistence.RefreshTokenRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
  private final RefreshTokenRepository refreshTokenRepository;
  private final RefreshTokenProperties refreshTokenProperties;
  private final Clock clock;

  public RefreshTokenServiceImpl(
      RefreshTokenRepository refreshTokenRepository,
      RefreshTokenProperties refreshTokenProperties,
      Clock clock) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.refreshTokenProperties = refreshTokenProperties;
    this.clock = clock;
  }

  @Override
  @Transactional
  public String issue(UUID userId) {
    String rawToken = OpaqueTokens.generate();
    refreshTokenRepository.save(
        RefreshToken.builder()
            .userId(userId)
            .tokenHash(sha256Hex(rawToken))
            .expiresAt(OffsetDateTime.now(clock).plus(refreshTokenProperties.ttl()))
            .build());
    return rawToken;
  }

  @Override
  @Transactional
  public Optional<UUID> rotate(String rawToken) {
    if (StringUtils.isBlank(rawToken)) {
      return Optional.empty();
    }
    var tokenOpt = refreshTokenRepository.findByTokenHash(sha256Hex(rawToken));
    if (tokenOpt.isEmpty()) {
      return Optional.empty();
    }
    var token = tokenOpt.get();
    if (token.isRevoked()) {
      refreshTokenRepository.revokeAllActiveByUserId(token.getUserId());
      return Optional.empty();
    }
    if (!token.getExpiresAt().isAfter(OffsetDateTime.now(clock))) { // expired when expiresAt <= now
      return Optional.empty();
    }
    token.setRevoked(true);
    return Optional.of(token.getUserId());
  }

  @Override
  @Transactional
  public void revoke(String rawToken) {
    if (StringUtils.isBlank(rawToken)) {
      return;
    }
    refreshTokenRepository
        .findByTokenHash(sha256Hex(rawToken))
        .ifPresent(token -> token.setRevoked(true));
  }

  @Override
  @Transactional
  public void revokeAllForUser(UUID userId) {
    refreshTokenRepository.revokeAllActiveByUserId(userId);
  }
}
