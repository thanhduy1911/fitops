package com.fitops.identity.application.service;

import com.fitops.commons.security.JwtProperties;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenMinterImpl implements AccessTokenMinter {
  private static final String TOKEN_TYPE = "Bearer";

  private final JwtIssuer jwtIssuer;
  private final JwtProperties jwtProperties;

  public AccessTokenMinterImpl(JwtIssuer jwtIssuer, JwtProperties jwtProperties) {
    this.jwtIssuer = jwtIssuer;
    this.jwtProperties = jwtProperties;
  }

  @Override
  public MintedAccessToken mint(UUID userId, Set<String> roles) {
    var accessToken = jwtIssuer.generate(userId, roles);
    var expiresIn = jwtProperties.accessTokenTtl().toSeconds();
    return new MintedAccessToken(accessToken, TOKEN_TYPE, expiresIn);
  }
}
