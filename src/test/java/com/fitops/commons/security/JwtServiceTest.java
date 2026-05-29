package com.fitops.commons.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class JwtServiceTest {

  private static final String SECRET = "test-secret-do-not-use-in-prod-pls-32+";
  private static final String ISSUER = "fitops";

  private final JwtService service = newService(Duration.ofMinutes(15));

  private JwtService newService(Duration ttl) {
    return new JwtService(new JwtProperties(ISSUER, SECRET, ttl));
  }

  @Test
  void generate_then_parse_returns_principal_with_userId_and_roles() {
    UUID userId = UUID.randomUUID();
    Set<String> roles = Set.of("ROLE_USER", "ROLE_ADMIN");

    String token = service.generate(userId, roles);
    Optional<JwtPrincipal> parsed = service.parse(token);

    assertThat(parsed).isPresent();
    assertThat(parsed.get().userId()).isEqualTo(userId);
    assertThat(parsed.get().roles()).containsExactlyInAnyOrderElementsOf(roles);
  }

  @Test
  void parse_expired_token_returns_empty() throws InterruptedException {
    JwtService shortTtl = newService(Duration.ofMillis(1));
    String token = shortTtl.generate(UUID.randomUUID(), Set.of("ROLE_USER"));
    Thread.sleep(50);

    assertThat(shortTtl.parse(token)).isEmpty();
  }

  @Test
  void parse_tampered_signature_returns_empty() {
    String token = service.generate(UUID.randomUUID(), Set.of("ROLE_USER"));
    String tampered = token.substring(0, token.length() - 2) + "xx";

    assertThat(service.parse(tampered)).isEmpty();
  }

  @Test
  void parse_wrong_issuer_returns_empty() {
    JwtService other =
        new JwtService(new JwtProperties("not-fitops", SECRET, Duration.ofMinutes(15)));
    String token = other.generate(UUID.randomUUID(), Set.of("ROLE_USER"));

    assertThat(service.parse(token)).isEmpty();
  }

  @Test
  void parse_alg_none_token_returns_empty() {
    // header `{"alg":"none","typ":"JWT"}` + payload `{"sub":"x"}` + empty signature
    String unsigned = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJ4In0.";
    assertThat(service.parse(unsigned)).isEmpty();
  }

  @Test
  void parse_malformed_string_returns_empty() {
    assertThat(service.parse("not-a-jwt")).isEmpty();
  }

  @Test
  void parse_null_returns_empty() {
    assertThat(service.parse(null)).isEmpty();
  }

  @Test
  void parse_blank_returns_empty() {
    assertThat(service.parse("  ")).isEmpty();
  }

  @Test
  void principal_authorities_returns_one_GrantedAuthority_per_role() {
    JwtPrincipal p = new JwtPrincipal(UUID.randomUUID(), Set.of("ROLE_USER", "ROLE_ADMIN"));
    assertThat(p.authorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }
}
