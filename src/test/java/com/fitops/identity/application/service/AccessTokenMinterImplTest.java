package com.fitops.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitops.commons.security.JwtProperties;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessTokenMinterImplTest {
  private static final String ROLE_USER = "ROLE_USER";
  private static final String ROLE_ADMIN = "ROLE_ADMIN";

  @Mock private JwtIssuer jwtIssuer;
  @Mock private JwtProperties jwtProperties;

  @Test
  void mint_composesIssuerAndTtl_intoBearerEnvelope() {
    var userId = UUID.randomUUID();
    var roles = Set.of(ROLE_USER, ROLE_ADMIN);
    when(jwtIssuer.generate(userId, roles)).thenReturn("signed-jwt");
    when(jwtProperties.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));

    var minter = new AccessTokenMinterImpl(jwtIssuer, jwtProperties);
    var minted = minter.mint(userId, roles);

    assertThat(minted.accessToken()).isEqualTo("signed-jwt");
    assertThat(minted.tokenType()).isEqualTo("Bearer");
    assertThat(minted.expiresIn()).isEqualTo(900L);
    verify(jwtIssuer).generate(userId, roles);
  }
}
