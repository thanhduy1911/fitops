package com.fitops.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fitops.commons.security.OpaqueTokens;
import com.fitops.identity.application.port.PasswordResetMailer;
import com.fitops.identity.domain.entity.PasswordResetToken;
import com.fitops.identity.domain.entity.RefreshToken;
import com.fitops.identity.infrastructure.persistence.PasswordResetTokenRepository;
import com.fitops.identity.infrastructure.persistence.RefreshTokenRepository;
import com.fitops.identity.infrastructure.persistence.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(PasswordResetIntegrationTest.CapturingMailer.class)
class PasswordResetIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:18-alpine").withReuse(true);

  @Value("${fitops.security.refresh-token.cookie-name}")
  String cookieName;

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;
  @Autowired RefreshTokenRepository refreshTokenRepository;
  @Autowired PasswordResetTokenRepository passwordResetTokenRepository;
  @Autowired CapturingMailer mailer;

  @BeforeEach
  void clean() {
    passwordResetTokenRepository.deleteAll();
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
    mailer.lastToken = null;
  }

  @Test
  void forgot_unknownEmail_returns200_noTokenCreated() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                      {"email":"ghost@fitops.com"}
                      """))
        .andExpect(status().isOk());

    assertThat(passwordResetTokenRepository.findAll()).isEmpty();
    assertThat(mailer.lastToken).isNull();
  }

  @Test
  void forgot_knownEmail_returns200_createsToken_invokesMailer() throws Exception {
    register();

    forgotPassword("john.doe@fitops.com");

    assertThat(passwordResetTokenRepository.findAll()).hasSize(1);
    assertThat(mailer.lastToken).isNotBlank();
  }

  @Test
  void reset_validToken_changesPassword_revokesRefreshTokens_consumesToken() throws Exception {
    var loginCookie = loginAndGetCookie(); // also creates a refresh token
    forgotPassword("john.doe@fitops.com");
    String resetToken = mailer.lastToken;

    resetPassword(resetToken, "brandNewPass123").andExpect(status().isOk());

    // every refresh token revoked; old refresh cookie now 401
    assertThat(refreshTokenRepository.findAll()).allMatch(RefreshToken::isRevoked);
    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(loginCookie))
        .andExpect(status().isUnauthorized());

    // new password works, old fails
    login("john.doe@fitops.com", "brandNewPass123").andExpect(status().isOk());
    login("john.doe@fitops.com", "password123").andExpect(status().isUnauthorized());

    // token single-use: replaying it -> 400 AUTH_009
    resetPassword(resetToken, "anotherPass123")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("AUTH_009"));
  }

  @Test
  void reset_expiredToken_returns400_auth009() throws Exception {
    register();
    var userId = userRepository.findByEmail("john.doe@fitops.com").orElseThrow().getId();
    String raw = "expired-reset-token";
    passwordResetTokenRepository.save(
        PasswordResetToken.builder()
            .userId(userId)
            .tokenHash(OpaqueTokens.sha256Hex(raw))
            .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1))
            .build());

    resetPassword(raw, "brandNewPass123")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("AUTH_009"));
  }

  @Test
  void reset_unknownToken_returns400_auth009() throws Exception {
    resetPassword("never-issued", "brandNewPass123")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("AUTH_009"));
  }

  @Test
  void forgot_twice_invalidatesPriorToken() throws Exception {
    register();
    forgotPassword("john.doe@fitops.com");
    String firstToken = mailer.lastToken;
    forgotPassword("john.doe@fitops.com");
    String secondToken = mailer.lastToken;

    // first link is dead, second works
    resetPassword(firstToken, "brandNewPass123")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("AUTH_009"));
    resetPassword(secondToken, "brandNewPass123").andExpect(status().isOk());
  }

  // --- helpers ---

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

  private void forgotPassword(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
        .andExpect(status().isOk());
  }

  private org.springframework.test.web.servlet.ResultActions resetPassword(
      String token, String newPassword) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + newPassword + "\"}"));
  }

  private org.springframework.test.web.servlet.ResultActions login(String email, String password)
      throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
  }

  private Cookie loginAndGetCookie() throws Exception {
    register();
    var response =
        login("john.doe@fitops.com", "password123")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return response.getCookie(cookieName);
  }

  @TestConfiguration
  static class CapturingMailer implements PasswordResetMailer {
    volatile String lastToken;

    @Bean
    @Primary
    PasswordResetMailer capturingMailer() {
      return this;
    }

    @Override
    public void sendResetLink(String email, String rawToken, OffsetDateTime expiresAt) {
      this.lastToken = rawToken;
    }
  }
}
