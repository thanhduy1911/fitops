package com.fitops.commons.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
  static final String REQUEST_ID_HEADER = "X-Request-Id";
  static final String REQUEST_ID_MDC_KEY = "request_id";

  // alphanumeric and dashes only, prevent log injection
  private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String requestId = resolveRequestId(request);
      MDC.put(REQUEST_ID_MDC_KEY, requestId);
      response.setHeader(REQUEST_ID_HEADER, requestId);
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(REQUEST_ID_MDC_KEY);
    }
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    return request.getRequestURI().startsWith("/actuator/health");
  }

  private String resolveRequestId(HttpServletRequest request) {
    String incomingRequestId = request.getHeader(REQUEST_ID_HEADER);
    if (StringUtils.isNotBlank(incomingRequestId)
        && VALID_REQUEST_ID.matcher(incomingRequestId).matches()) {
      return incomingRequestId;
    }
    return UUID.randomUUID().toString();
  }
}
