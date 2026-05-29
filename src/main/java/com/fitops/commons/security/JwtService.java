package com.fitops.commons.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
  private static final String ROLES_CLAIM = "roles";

  private final JwtProperties jwtProperties;
  private final SecretKey key;

  public JwtService(JwtProperties jwtProperties) {
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

  public Optional<JwtPrincipal> parse(String token) {
    if (StringUtils.isBlank(token)) {
      return Optional.empty();
    }
    try {
      var claims =
          Jwts.parser()
              .verifyWith(key)
              .requireIssuer(jwtProperties.issuer())
              .build()
              .parseSignedClaims(token)
              .getPayload();
      var userId = UUID.fromString(claims.getSubject());
      var roles = extractRoles(claims);
      return Optional.of(new JwtPrincipal(userId, roles));
    } catch (JwtException | IllegalArgumentException ex) {
      logger.debug("JWT parse rejected: {}", ex.getClass().getSimpleName());
      return Optional.empty();
    }
  }

  private Set<String> extractRoles(Claims claims) {
    Object raw = claims.get(ROLES_CLAIM);
    if (raw instanceof List<?> list) {
      Set<String> out = new HashSet<>(list.size());
      for (Object o : list) {
        if (o instanceof String s) out.add(s);
      }
      return out;
    }
    return Set.of();
  }
}
