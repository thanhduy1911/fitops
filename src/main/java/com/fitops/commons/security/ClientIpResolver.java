package com.fitops.commons.security;

import com.fitops.commons.constants.ServiceHeader;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {
  private static final String FORWARDED_FOR_HEADER =
      ServiceHeader.FORWARDED_FOR_HEADER.getHeaderName();

  /**
   * Resolve the caller IP as observed by the trusted edge proxy. Behind a single reverse proxy the
   * only trustworthy value in {@code X-Forwarded-For} is the rightmost entry, the hop the proxy
   * itself appended. A client can pre-seed the header, so anything left of that is
   * attacker-controlled and ignored. Falls back to {@code getRemoteAddr()} when no proxy is
   * present.
   */
  public String resolve(HttpServletRequest request) {
    Enumeration<String> lines = request.getHeaders(FORWARDED_FOR_HEADER);
    String rightmost = null;
    while (lines.hasMoreElements()) {
      for (String hop : lines.nextElement().split(",")) {
        String trimmed = hop.trim();
        if (StringUtils.isNotBlank(trimmed)) {
          rightmost = trimmed;
        }
      }
    }
    return rightmost != null ? rightmost : request.getRemoteAddr();
  }
}
