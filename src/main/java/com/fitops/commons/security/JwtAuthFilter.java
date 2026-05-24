package com.fitops.commons.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
  private static final String AUTH_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    try {
      var header = request.getHeader(AUTH_HEADER);
      if (header != null && header.startsWith(BEARER_PREFIX)) {
        var token = header.substring(BEARER_PREFIX.length());
        jwtService
            .parse(token)
            .ifPresent(
                principal -> {
                  var auth =
                      new UsernamePasswordAuthenticationToken(
                          principal.userId(), null, principal.authorities());
                  SecurityContextHolder.getContext().setAuthentication(auth);
                });
        filterChain.doFilter(request, response);
      }
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
