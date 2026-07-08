package com.fitops.commons.security;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Forwards an exception into Spring MVC's shared {@code handlerExceptionResolver} so it is rendered
 * as an RFC-7807 {@link org.springframework.http.ProblemDetail} by {@code GlobalExceptionHandler}.
 *
 * <p>Concentrates the {@code @Qualifier("handlerExceptionResolver")} magic string to one place, a
 * typo there would silently bypass RFC-7807 shaping.
 */
@Component
public class ProblemDetailResolver {
  private final HandlerExceptionResolver exceptionResolver;

  public ProblemDetailResolver(
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
    this.exceptionResolver = exceptionResolver;
  }

  public void render(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull Exception exception) {
    exceptionResolver.resolveException(request, response, null, exception);
  }
}
