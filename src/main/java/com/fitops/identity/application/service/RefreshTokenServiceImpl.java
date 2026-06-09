package com.fitops.identity.application.service;

import com.fitops.commons.security.RefreshTokenProperties;
import com.fitops.identity.domain.entity.RefreshToken;
import com.fitops.identity.infrastructure.persistence.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
  private static final int TOKEN_BYTES = 32; // 256 bits
  private final RefreshTokenRepository refreshTokenRepository;
  private final RefreshTokenProperties refreshTokenProperties;
  private final SecureRandom secureRandom = new SecureRandom();

  public RefreshTokenServiceImpl(
      RefreshTokenRepository refreshTokenRepository,
      RefreshTokenProperties refreshTokenProperties) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.refreshTokenProperties = refreshTokenProperties;
  }

  @Override
  @Transactional
  public String issue(UUID userId) {
    byte[] raw = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(raw);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    refreshTokenRepository.save(
        RefreshToken.builder()
            .userId(userId)
            .tokenHash(sha256Hex(rawToken))
            .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plus(refreshTokenProperties.ttl()))
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
    if (token.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
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

  private static String sha256Hex(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
