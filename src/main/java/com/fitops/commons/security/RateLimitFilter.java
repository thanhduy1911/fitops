package com.fitops.commons.security;

import com.fitops.commons.exception.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
  private static final Set<String> PROTECTED_PATHS =
      Set.of("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/forgot-password");
  private final RateLimitProperties props;
  private final AuthRateLimiter rateLimiter;
  private final ClientIpResolver clientIpResolver;
  private final HandlerExceptionResolver exceptionResolver;

  public RateLimitFilter(
      RateLimitProperties props,
      AuthRateLimiter rateLimiter,
      ClientIpResolver clientIpResolver,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
    this.props = props;
    this.rateLimiter = rateLimiter;
    this.clientIpResolver = clientIpResolver;
    this.exceptionResolver = exceptionResolver;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String key = request.getRequestURI() + ":" + clientIpResolver.resolve(request);
    var probe = rateLimiter.tryConsume(key);
    if (probe.isConsumed()) {
      filterChain.doFilter(request, response);
      return;
    }
    long retryAfter = Math.max(1, Math.ceilDiv(probe.getNanosToWaitForRefill(), 1_000_000_000L));
    exceptionResolver.resolveException(
        request, response, null, new RateLimitExceededException(retryAfter));
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    return !props.enabled()
        || !HttpMethod.POST.matches(request.getMethod())
        || !PROTECTED_PATHS.contains(request.getRequestURI());
  }
}
