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
   * Resolve the caller IP from {@code X-Forwarded-For}, taking the rightmost non-blank entry across
   * all header lines (the hop the trusted edge proxy appended; anything to its left is
   * client-supplied and forgeable). All lines are read because a proxy may append a separate header
   * line rather than comma-joining. Falls back to {@code getRemoteAddr()} when no usable header is
   * present.
   */
  public String resolve(HttpServletRequest request) {
    String clientIp = null;
    Enumeration<String> headerLines = request.getHeaders(FORWARDED_FOR_HEADER);
    if (headerLines != null) {
      while (headerLines.hasMoreElements()) {
        for (String token : headerLines.nextElement().split(",")) {
          String trimmed = token.trim();
          if (StringUtils.isNotBlank(trimmed)) {
            clientIp = trimmed;
          }
        }
      }
    }
    return clientIp != null ? clientIp : request.getRemoteAddr();
  }
}
