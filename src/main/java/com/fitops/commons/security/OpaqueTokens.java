package com.fitops.commons.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class OpaqueTokens {
  private static final int TOKEN_BYTES = 32; // 256 bits
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private OpaqueTokens() {}

  public static String generate() {
    byte[] raw = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(raw);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
  }

  public static String sha256Hex(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
