package com.fitops.commons.security;

import com.fitops.commons.constants.ServiceHeader;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {
  private static final String FORWARDED_FOR_HEADER =
      ServiceHeader.FORWARDED_FOR_HEADER.getHeaderName();

  /**
   * Resolve the caller IP as observed by the trusted edge proxy. Behind a single reverse proxy the
   * only trustworthy value in {@code X-Forwarded-For} is the rightmost entry — the hop the proxy
   * itself appended. A client can pre-seed the header, so anything left of that is
   * attacker-controlled and ignored. Falls back to {@code getRemoteAddr()} when no proxy is
   * present.
   */
  public String resolve(HttpServletRequest request) {
    String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
    if (StringUtils.isNotBlank(forwardedFor)) {
      String[] hops = forwardedFor.split(",");
      String rightmost = hops[hops.length - 1].trim();
      if (StringUtils.isNotBlank(rightmost)) {
        return rightmost;
      }
    }
    return request.getRemoteAddr();
  }
}
