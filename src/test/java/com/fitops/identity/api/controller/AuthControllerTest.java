package com.fitops.identity.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitops.commons.constants.MDCConstant;
import com.fitops.commons.exception.GlobalExceptionHandler;
import com.fitops.commons.security.JwtService;
import com.fitops.identity.api.response.AuthResponse;
import com.fitops.identity.application.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
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
@Import({GlobalExceptionHandler.class, AuthController.class})
class AuthControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean AuthService authService;
  @MockitoBean JwtService jwtService;

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
    when(authService.register(any())).thenReturn(new AuthResponse("jwt-token", "Bearer", 3600L));

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
}
