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
import java.util.UUID;
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
    return "";
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
