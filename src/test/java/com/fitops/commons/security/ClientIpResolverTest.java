package com.fitops.commons.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.commons.constants.ServiceHeader;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class ClientIpResolverTest {
  private final ClientIpResolver resolver = new ClientIpResolver();

  @Test
  void spoofedLeftmostXff_resolvesToRightmostTrustedEntry() {
    var request = new MockHttpServletRequest();
    request.addHeader(ServiceHeader.FORWARDED_FOR_HEADER.getHeaderName(), "1.2.3.4, 203.0.113.7");
    assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
  }

  @Test
  void singleXffEntry_resolvesToThatEntry() {
    var request = new MockHttpServletRequest();
    request.addHeader(ServiceHeader.FORWARDED_FOR_HEADER.getHeaderName(), "203.0.113.7");
    assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
  }

  @Test
  void noXffHeader_fallsBackToRemoteAddress() {
    var request = new MockHttpServletRequest();
    request.setRemoteAddr("198.51.100.42");
    assertThat(resolver.resolve(request)).isEqualTo("198.51.100.42");
  }

  @Test
  void blankXffHeader_fallsBackToRemoteAddress() {
    var request = new MockHttpServletRequest();
    request.addHeader(ServiceHeader.FORWARDED_FOR_HEADER.getHeaderName(), "    ");
    request.setRemoteAddr("198.51.100.42");
    assertThat(resolver.resolve(request)).isEqualTo("198.51.100.42");
  }

  @Test
  void repeatedXffHeaders_resolvesToRightmostTrustedEntry() {
    var request = new MockHttpServletRequest();
    request.addHeader(ServiceHeader.FORWARDED_FOR_HEADER.getHeaderName(), "1.2.3.4");
    request.addHeader(ServiceHeader.FORWARDED_FOR_HEADER.getHeaderName(), "203.0.113.7");
    assertThat(resolver.resolve(request)).isEqualTo("203.0.113.7");
  }
}
