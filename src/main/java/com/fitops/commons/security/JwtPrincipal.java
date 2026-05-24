package com.fitops.commons.security;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record JwtPrincipal(UUID userId, Set<String> roles) {
  public Collection<? extends GrantedAuthority> authorities() {
    return roles.stream()
        .map(SimpleGrantedAuthority::new)
        .map(GrantedAuthority.class::cast)
        .toList();
  }
}
