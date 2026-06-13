package com.fitops.commons.security;

import com.fitops.commons.constants.ServiceHeader;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {
  private static final String FORWARDED_FOR_HEADER =
      ServiceHeader.FORWARDED_FOR_HEADER.getHeaderName();

  private final List<String> trustedProxies;

  public ClientIpResolver() {
    this("");
  }

  public ClientIpResolver(
      @Value("${security.trusted-proxies:}") String trustedProxiesConfig) {
    this.trustedProxies =
        trustedProxiesConfig.isEmpty()
            ? Collections.emptyList()
            : Arrays.asList(trustedProxiesConfig.split(","));
  }

  /**
   * Resolve the caller IP. Only honors {@code X-Forwarded-For} when the immediate peer (from
   * {@code getRemoteAddr()}) is a configured trusted proxy. When trusted, returns the rightmost
   * non-proxy entry from the header. Otherwise returns {@code getRemoteAddr()} directly.
   */
  public String resolve(HttpServletRequest request) {
    String remoteAddr = request.getRemoteAddr();

    // Only trust X-Forwarded-For if the immediate peer is a trusted proxy
    if (!isTrustedProxy(remoteAddr)) {
      return remoteAddr;
    }

    // Parse X-Forwarded-For and extract the rightmost non-proxy IP
    String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
    if (StringUtils.isNotBlank(forwardedFor)) {
      String[] hops = forwardedFor.split(",");
      // Iterate right-to-left to find the first non-proxy entry
      for (int i = hops.length - 1; i >= 0; i--) {
        String trimmed = hops[i].trim();
        if (StringUtils.isNotBlank(trimmed) && !isTrustedProxy(trimmed)) {
          // Return the rightmost non-proxy entry - the original client IP
          return trimmed;
        }
      }
    }

    // Fall back to remote address if header is absent, empty, or malformed
    return remoteAddr;
  }

  /**
   * Check whether the given IP address is in the list of trusted proxies.
   *
   * @param ip the IP address to check
   * @return true if the IP is a trusted proxy, false otherwise
   */
  private boolean isTrustedProxy(String ip) {
    if (ip == null || trustedProxies.isEmpty()) {
      return false;
    }
    // Trim configured proxies and compare
    return trustedProxies.stream().anyMatch(proxy -> proxy.trim().equals(ip));
  }
}
