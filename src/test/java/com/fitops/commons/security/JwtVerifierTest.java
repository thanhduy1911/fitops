package com.fitops.commons.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class JwtVerifierTest {

  private static final String SECRET = "test-secret-do-not-use-in-prod-pls-32+";
  private static final String ISSUER = "fitops";

  private final JwtProperties props = new JwtProperties(ISSUER, SECRET, Duration.ofMinutes(15));
  private final JwtVerifier verifier = new JwtVerifier(props);
  private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

  private String signed(String issuer, UUID sub, Set<String> roles, Instant iat, Duration ttl) {
    return Jwts.builder()
        .issuer(issuer)
        .subject(sub.toString())
        .claim("roles", List.copyOf(roles))
        .issuedAt(Date.from(iat))
        .expiration(Date.from(iat.plus(ttl)))
        .signWith(key)
        .compact();
  }

  @Test
  void parse_valid_token_returns_principal_with_userId_and_roles() {
    UUID userId = UUID.randomUUID();
    Set<String> roles = Set.of("ROLE_USER", "ROLE_ADMIN");
    String token = signed(ISSUER, userId, roles, Instant.now(), Duration.ofMinutes(15));

    Optional<JwtPrincipal> parsed = verifier.parse(token);

    assertThat(parsed).isPresent();
    assertThat(parsed.get().userId()).isEqualTo(userId);
    assertThat(parsed.get().roles()).containsExactlyInAnyOrderElementsOf(roles);
  }

  @Test
  void parse_expired_token_returns_empty() {
    String token =
        signed(
            ISSUER,
            UUID.randomUUID(),
            Set.of("ROLE_USER"),
            Instant.now().minusSeconds(3600),
            Duration.ofSeconds(1));
    assertThat(verifier.parse(token)).isEmpty();
  }

  @Test
  void parse_tampered_signature_returns_empty() {
    String token =
        signed(
            ISSUER, UUID.randomUUID(), Set.of("ROLE_USER"), Instant.now(), Duration.ofMinutes(15));
    String tampered = token.substring(0, token.length() - 2) + "xx";
    assertThat(verifier.parse(tampered)).isEmpty();
  }

  @Test
  void parse_wrong_issuer_returns_empty() {
    String token =
        signed(
            "not-fitops",
            UUID.randomUUID(),
            Set.of("ROLE_USER"),
            Instant.now(),
            Duration.ofMinutes(15));
    assertThat(verifier.parse(token)).isEmpty();
  }

  @Test
  void parse_alg_none_token_returns_empty() {
    String unsigned = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJ4In0.";
    assertThat(verifier.parse(unsigned)).isEmpty();
  }

  @Test
  void parse_malformed_string_returns_empty() {
    assertThat(verifier.parse("not-a-jwt")).isEmpty();
  }

  @Test
  void parse_null_returns_empty() {
    assertThat(verifier.parse(null)).isEmpty();
  }

  @Test
  void parse_blank_returns_empty() {
    assertThat(verifier.parse("  ")).isEmpty();
  }

  @Test
  void principal_authorities_returns_one_GrantedAuthority_per_role() {
    JwtPrincipal p = new JwtPrincipal(UUID.randomUUID(), Set.of("ROLE_USER", "ROLE_ADMIN"));
    assertThat(p.authorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }
}
