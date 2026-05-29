package com.fitops.commons.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityIntegrationTest.TestEndpoints.class)
@Testcontainers
public class SecurityIntegrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgreSQLContainer =
      new PostgreSQLContainer("postgres:18-alpine").withReuse(true);

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;

  @TestConfiguration
  static class TestEndpoints {
    @RestController
    static class PingController {
      @GetMapping("/test/ping")
      String ping() {
        return "pong";
      }
    }
  }

  @Test
  void unauth_GET_test_ping_returns_401_AUTH_001_with_requestId_in_body() throws Exception {
    mockMvc
        .perform(get("/test/ping"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_001"))
        .andExpect(jsonPath("$.requestId").exists())
        .andExpect(jsonPath("$.title").value("Authentication required"));
  }

  @Test
  void authed_GET_test_ping_returns_200_pong() throws Exception {
    String token = jwtService.generate(UUID.randomUUID(), Set.of("ROLE_USER"));
    mockMvc
        .perform(get("/test/ping").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(content().string("pong"));
  }

  @Test
  void bogus_bearer_returns_401_AUTH_001() throws Exception {
    mockMvc
        .perform(get("/test/ping").header("Authorization", "Bearer bogus"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_001"));
  }

  @Test
  void public_actuator_health_returns_200_without_token() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void cors_preflight_OPTIONS_returns_200_with_allow_credentials() throws Exception {
    mockMvc
        .perform(
            options("/test/ping")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
        .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
  }
}
