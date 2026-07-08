package com.fitops.commons.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final ProblemDetailResolver problemDetailResolver;

  public ProblemDetailAuthenticationEntryPoint(ProblemDetailResolver problemDetailResolver) {
    this.problemDetailResolver = problemDetailResolver;
  }

  @Override
  public void commence(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull AuthenticationException authException) {
    problemDetailResolver.render(request, response, authException);
  }
}
