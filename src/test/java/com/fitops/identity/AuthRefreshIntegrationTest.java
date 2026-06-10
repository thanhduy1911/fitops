package com.fitops.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fitops.identity.domain.entity.RefreshToken;
import com.fitops.identity.infrastructure.persistence.RefreshTokenRepository;
import com.fitops.identity.infrastructure.persistence.UserRepository;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(AuthRefreshIntegrationTest.ProbeEndpoint.class)
class AuthRefreshIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:18-alpine").withReuse(true);

  @org.springframework.beans.factory.annotation.Value(
      "${fitops.security.refresh-token.cookie-name}")
  String cookieName;

  @Autowired MockMvc mockMvc;

  @Autowired UserRepository userRepository;
  @Autowired RefreshTokenRepository refreshTokenRepository;
  @Autowired ObjectMapper objectMapper;

  @Test
  void refresh_validCookie_rotatesPair_oldRejected_newAuthenticates() throws Exception {
    var cookieA = loginAndGetCookie();

    var refreshResponse =
        mockMvc
            .perform(post("/api/v1/auth/refresh").cookie(cookieA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(cookie().exists(cookieName))
            .andReturn()
            .getResponse();
    var cookieB = refreshResponse.getCookie(cookieName);
    assert cookieB != null;
    assertThat(cookieB.getValue()).isNotEqualTo(cookieA.getValue());

    // old token now rejected
    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(cookieA))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_003"));

    // new access token authenticates
    String accessToken =
        objectMapper.readTree(refreshResponse.getContentAsString()).get("accessToken").asString();
    mockMvc
        .perform(get("/test/ping").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk());
  }

  @Test
  void refresh_replayOfRevokedToken_revokesEntireFamily_returns401() throws Exception {
    var cookieA = loginAndGetCookie();
    var cookieB =
        mockMvc
            .perform(post("/api/v1/auth/refresh").cookie(cookieA))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getCookie(cookieName);
    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(Objects.requireNonNull(cookieB)))
        .andExpect(status().isOk()); // -> cookieC active; A,B revoked

    // replay the already-revoked A -> theft signal
    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(cookieA))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_003"));

    // family burned: every token for the user is revoked (incl. the still-fresh C)
    assertThat(refreshTokenRepository.findAll()).isNotEmpty().allMatch(RefreshToken::isRevoked);
  }

  @Test
  void logout_revokesToken_clearsCookie_returns200_andRefreshThenFails() throws Exception {
    var cookieA = loginAndGetCookie();

    var logoutResponse =
        mockMvc
            .perform(post("/api/v1/auth/logout").cookie(cookieA))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(Objects.requireNonNull(logoutResponse.getCookie(cookieName)).getMaxAge()).isZero();

    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(cookieA))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_003"));
  }

  @Test
  void logout_noCookie_isIdempotent_returns200() throws Exception {
    mockMvc
        .perform(post("/api/v1/auth/logout"))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(cookieName, 0));
  }

  @Test
  void refresh_expiredToken_returns401() throws Exception {
    register();
    var userId = userRepository.findByEmail("john.doe@fitops.com").orElseThrow().getId();
    String raw = "expired-raw-token";
    refreshTokenRepository.save(
        RefreshToken.builder()
            .userId(userId)
            .tokenHash(sha256Hex(raw))
            .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1))
            .build());

    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(new Cookie(cookieName, raw)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_003"));
  }

  @Test
  void refresh_afterUserDeactivated_returns401() throws Exception {
    var cookieA = loginAndGetCookie();
    var user = userRepository.findByEmail("john.doe@fitops.com").orElseThrow();
    user.setActive(false);
    userRepository.save(user);

    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(cookieA))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_003"));
  }

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

  @BeforeEach
  void clean() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  private void register() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                     {"email":"john.doe@fitops.com","username":"john_doe","password":"password123","displayName":"john_doe"}
                     """))
        .andExpect(status().isCreated());
  }

  private Cookie loginAndGetCookie() throws Exception {
    register();
    var response =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"john.doe@fitops.com","password":"password123"}
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return response.getCookie(cookieName);
  }

  private static String sha256Hex(String value) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
