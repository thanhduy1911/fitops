package com.fitops.identity.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fitops.commons.constants.ErrorCode;
import com.fitops.commons.constants.MDCConstant;
import com.fitops.commons.exception.BadRequestException;
import com.fitops.commons.exception.GlobalExceptionHandler;
import com.fitops.commons.security.JwtVerifier;
import com.fitops.commons.security.RateLimitFilter;
import com.fitops.identity.application.service.AuthService;
import com.fitops.identity.application.service.LoginResult;
import com.fitops.identity.application.service.MintedAccessToken;
import com.fitops.identity.config.RefreshTokenProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {
      SecurityAutoConfiguration.class,
      UserDetailsServiceAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@EnableConfigurationProperties(RefreshTokenProperties.class)
@Import({GlobalExceptionHandler.class, AuthController.class})
class AuthControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private RefreshTokenProperties refreshTokenProperties;
  @MockitoBean AuthService authService;
  @MockitoBean RateLimitFilter rateLimitFilter;
  @MockitoBean JwtVerifier jwtVerifier;

  @BeforeEach
  void setMdc() {
    MDC.put(MDCConstant.REQUEST_ID.getKey(), "request-id-1");
  }

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void register_validRequest_returns201_withTokenBody() throws Exception {
    when(authService.register(any()))
        .thenReturn(new MintedAccessToken("jwt-token", "Bearer", 3600L));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"email":"joe@fitops.com","username":"joe_doe","password":"password123","displayName":"Joe"}
                                        """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").value("jwt-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(3600));
  }

  @Test
  void register_invalidRequest_returns400_general001() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"email":"not-an-email","username":"ab","password":"short","displayName":""}
                                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("GENERAL_001"))
        .andExpect(jsonPath("$.violations").isArray());
  }

  @Test
  void register_emptyBody_returns400_general001() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("GENERAL_001"));
  }

  @Test
  void login_validRequest_returns200_withTokenBodyAndRefreshCookie() throws Exception {
    when(authService.login(any())).thenReturn(new LoginResult(new MintedAccessToken("jwt-token", "Bearer", 3600L), "raw-token"));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                      {"email":"joe@fitops.com","password":"password123"}
                      """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("jwt-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(3600))
        .andExpect(cookie().value(refreshTokenProperties.cookieName(), "raw-token"))
        .andExpect(cookie().httpOnly(refreshTokenProperties.cookieName(), true))
        .andExpect(
            cookie().secure(refreshTokenProperties.cookieName(), refreshTokenProperties.secure()))
        .andExpect(
            cookie().path(refreshTokenProperties.cookieName(), refreshTokenProperties.cookiePath()))
        .andExpect(
            cookie()
                .maxAge(
                    refreshTokenProperties.cookieName(),
                    (int) refreshTokenProperties.ttl().toSeconds()))
        .andExpect(cookie().sameSite(refreshTokenProperties.cookieName(), "Strict"));
  }

  @Test
  void login_invalidRequest_returns400_general001() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                      {"email":"not-an-email","password":""}
                      """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("GENERAL_001"));
  }

  @Test
  void forgotPassword_validEmail_returns200() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"joe@fitops.com"}
                    """))
        .andExpect(status().isOk());
  }

  @Test
  void forgotPassword_invalidEmail_returns400_general001() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"not-an-email"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("GENERAL_001"));
  }

  @Test
  void resetPassword_validRequest_returns200() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"token":"some-raw-token","newPassword":"newpassword123"}
                    """))
        .andExpect(status().isOk());
  }

  @Test
  void resetPassword_weakPassword_returns400_general001() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"token":"some-raw-token","newPassword":"short"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("GENERAL_001"));
  }

  @Test
  void resetPassword_invalidToken_returns400_auth009() throws Exception {
    doThrow(new BadRequestException(ErrorCode.AUTH_009, "Password reset token invalid or expired"))
        .when(authService)
        .resetPassword(any());

    mockMvc
        .perform(
            post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"token":"bad-token","newPassword":"newpassword123"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("AUTH_009"));
  }
}
