package com.fitops.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fitops.commons.constants.ServiceHeader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@TestPropertySource(
    properties = {
      "fitops.security.rate-limit.enabled=true",
      "fitops.security.rate-limit.capacity=5",
      "fitops.security.rate-limit.refill-period=PT1M"
    })
class RateLimitIntegrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgreSQLContainer =
      new PostgreSQLContainer("postgres:18-alpine").withReuse(true);

  @Autowired MockMvc mockMvc;

  private ResultActions login(String ip) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/login")
            .header(ServiceHeader.FORWARDED_FOR_HEADER.getHeaderName(), ip)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"john.doe@fitops.com\",\"password\":\"password123\"}"));
  }

  private ResultActions register(String ip, String body) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/register")
            .header(ServiceHeader.FORWARDED_FOR_HEADER.getHeaderName(), ip)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private static String registerBody(int i) {
    return """
          {"email":"user%d@fitops.com","username":"user_%d","password":"password123","displayName":"User %d"}
          """
        .formatted(i, i, i);
  }

  private ResultActions forgotPassword(String ip, String email) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/forgot-password")
            .header(ServiceHeader.FORWARDED_FOR_HEADER.getHeaderName(), ip)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"%s\"}".formatted(email)));
  }

  private ResultActions refresh(String ip) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/refresh")
            .header(ServiceHeader.FORWARDED_FOR_HEADER.getHeaderName(), ip));
  }

  @Test
  void login_sixthRequest_returns429_withAuth006AndRetryAfter() throws Exception {
    String ip = "203.0.113.10";
    for (int i = 0; i < 5; i++) {
      login(ip).andExpect(status().isUnauthorized()); // AUTH_007, still consume a token
    }
    login(ip)
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.errorCode").value("AUTH_006"));
  }

  @Test
  void register_sixthRequest_returns429_withAuth006AndRetryAfter() throws Exception {
    String ip = "203.0.113.20";
    for (int i = 0; i < 5; i++) {
      register(ip, registerBody(i)).andExpect(status().isCreated());
    }
    register(ip, registerBody(5))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.errorCode").value("AUTH_006"));
  }

  @Test
  void forgotPassword_sixthRequest_returns429_withAuth006AndRetryAfter() throws Exception {
    String ip = "203.0.113.30";
    for (int i = 0; i < 5; i++) {
      forgotPassword(ip, "unknown@fitops.com").andExpect(status().isOk());
    }
    forgotPassword(ip, "unknown@fitops.com")
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.errorCode").value("AUTH_006"));
  }

  @Test
  void perEndpointIsolation_exhaustingLoginDoesNotLimitRegister() throws Exception {
    String ip = "203.0.113.40";
    for (int i = 0; i < 5; i++) {
      login(ip).andExpect(status().isUnauthorized());
    }
    login(ip).andExpect(status().isTooManyRequests()); // login bucket drained
    // register on the SAME ip uses a separate (endpoint, ip) bucket → unaffected
    register(ip, registerBody(0)).andExpect(status().isCreated());
  }

  @Test
  void perIpIsolation_limitOnOneIpDoesNotAffectAnother() throws Exception {
    String exhaustedIp = "203.0.113.50";
    String freshIp = "203.0.113.51";
    for (int i = 0; i < 5; i++) {
      login(exhaustedIp).andExpect(status().isUnauthorized());
    }
    login(exhaustedIp).andExpect(status().isTooManyRequests());
    // a different client IP has its own full bucket
    login(freshIp).andExpect(status().isUnauthorized());
  }

  @Test
  void refresh_isNeverRateLimited() throws Exception {
    String ip = "203.0.113.60";
    for (int i = 0; i < 10; i++) {
      refresh(ip).andExpect(status().isUnauthorized()); // AUTH_003, never 429
    }
  }

  @Test
  void malformedPayload_stillCountsTowardLimit() throws Exception {
    String ip = "203.0.113.70";
    for (int i = 0; i < 5; i++) {
      register(ip, "{}").andExpect(status().isBadRequest()); // validation 400, token still spent
    }
    register(ip, "{}")
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.errorCode").value("AUTH_006"));
  }
}
