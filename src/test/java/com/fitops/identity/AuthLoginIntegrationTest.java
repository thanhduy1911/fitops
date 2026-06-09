package com.fitops.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fitops.identity.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(AuthLoginIntegrationTest.ProbeEndpoint.class)
class AuthLoginIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:18-alpine").withReuse(true);

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;
  @Autowired ObjectMapper objectMapper;

  // === Configuration ===
  @TestConfiguration
  static class ProbeEndpoint {
    @RestController
    static class PingController {
      @GetMapping("/test/ping")
      String ping() {
        return "pong";
      }
    }
  }

  private void register() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                      {"email":"%s","username":"%s","password":"%s","displayName":"%s"}
                      """
                        .formatted("john.doe@fitops.com", "john_doe", "password123", "john_doe")))
        .andExpect(status().isCreated());
  }

  private static String loginJson(String email, String password) {
    return """
          {"email":"%s","password":"%s"}
          """
        .formatted(email, password);
  }

  @Test
  void login_correctCredentials_returns200_tokenAndRefreshCookie() throws Exception {
    register();
    var response =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("john.doe@fitops.com", "password123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(3600))
            .andExpect(cookie().sameSite("refreshToken", "Strict"))
            .andReturn()
            .getResponse();
    var cookie = response.getCookie("refreshToken");
    assertThat(cookie).isNotNull();
    assertThat(cookie.getValue()).isNotBlank();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getSecure()).isTrue();
    assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
  }

  @Test
  void login_wrongPassword_returns401_auth007() throws Exception {
    register();
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("john.doe@fitops.com", "wrong-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_007"));
  }

  @Test
  void login_unknownEmail_returns401_auth007() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("nobody@fitops.com", "password123")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_007"));
  }

  @Test
  void login_deactivatedUser_returns401_auth007() throws Exception {
    register();
    var user = userRepository.findByEmail("john.doe@fitops.com").orElseThrow();
    user.setActive(false);
    userRepository.save(user);
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("john.doe@fitops.com", "password123")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_007"));
  }

  @Test
  void login_issuedAccessToken_authenticatesOnProtectedEndpoint() throws Exception {
    register();
    var body =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("john.doe@fitops.com", "password123")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String accessToken = objectMapper.readTree(body).get("accessToken").asString();
    mockMvc
        .perform(get("/test/ping").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk());
  }
}
