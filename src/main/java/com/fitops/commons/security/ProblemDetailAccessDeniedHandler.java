package com.fitops.commons.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {
  private final ProblemDetailResolver problemDetailResolver;

  public ProblemDetailAccessDeniedHandler(ProblemDetailResolver problemDetailResolver) {
    this.problemDetailResolver = problemDetailResolver;
  }

  @Override
  public void handle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull AccessDeniedException accessDeniedException) {
    problemDetailResolver.render(request, response, accessDeniedException);
  }
}
