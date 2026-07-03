package com.fitops.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.commons.security.JwtPrincipal;
import com.fitops.commons.security.JwtProperties;
import com.fitops.commons.security.JwtVerifier;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtIssuerTest {

  private static final String SECRET = "test-secret-do-not-use-in-prod-pls-32+";
  private static final String ISSUER = "fitops";

  private final JwtProperties props = new JwtProperties(ISSUER, SECRET, Duration.ofMinutes(15));
  private final JwtIssuer issuer = new JwtIssuer(props);
  private final JwtVerifier verifier = new JwtVerifier(props);

  @Test
  void generated_token_is_accepted_by_verifier_with_userId_and_roles() {
    UUID userId = UUID.randomUUID();
    Set<String> roles = Set.of("ROLE_USER", "ROLE_ADMIN");

    String token = issuer.generate(userId, roles);
    Optional<JwtPrincipal> parsed = verifier.parse(token);

    assertThat(parsed).isPresent();
    assertThat(parsed.get().userId()).isEqualTo(userId);
    assertThat(parsed.get().roles()).containsExactlyInAnyOrderElementsOf(roles);
  }

  @Test
  void generated_token_carries_configured_issuer() {
    JwtVerifier otherIssuer =
        new JwtVerifier(new JwtProperties("not-fitops", SECRET, Duration.ofMinutes(15)));
    String token = issuer.generate(UUID.randomUUID(), Set.of("ROLE_USER"));
    assertThat(otherIssuer.parse(token)).isEmpty();
  }
}
