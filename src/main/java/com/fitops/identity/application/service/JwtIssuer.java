package com.fitops.identity.application.service;

import com.fitops.commons.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtIssuer {
  private static final String ROLES_CLAIM = "roles";

  private final JwtProperties jwtProperties;
  private final SecretKey key;

  public JwtIssuer(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String generate(UUID userId, Set<String> roles) {
    var now = Instant.now();
    var expire = now.plus(jwtProperties.accessTokenTtl());
    return Jwts.builder()
        .issuer(jwtProperties.issuer())
        .subject(userId.toString())
        .claim(ROLES_CLAIM, List.copyOf(roles))
        .issuedAt(Date.from(now))
        .expiration(Date.from(expire))
        .signWith(key)
        .compact();
  }
}
