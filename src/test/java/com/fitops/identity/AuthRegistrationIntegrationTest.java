package com.fitops.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class AuthRegistrationIntegrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:18-alpine").withReuse(true);

  @Autowired MockMvc mockMvc;

  private static String registerJson(
      String email, String username, String password, String displayName) {
    return """
      {"email":"%s","username":"%s","password":"%s","displayName":"%s"}
      """
        .formatted(email, username, password, displayName);
  }

  @Test
  void register_validRequest_returns201_withRealToken() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    registerJson("john.doe@fitops.com", "john_doe", "password123", "John Doe")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(3600));
  }

  @Test
  void register_duplicateEmail_returns409_auth004() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("dup@fitops.com", "first_user", "password123", "First")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("dup@fitops.com", "second_user", "password123", "Second")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("AUTH_004"));
  }

  @Test
  void register_duplicateUsernameDifferentCase_returns409_auth005() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("john.doe@fitops.com", "JohnD", "password123", "John Doe")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("jane.doe@fitops.com", "johnd", "password123", "Jane Doe")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("AUTH_005"));
  }

  @Test
  void register_invalidInput_returns400_general001() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("not-an-email", "ab", "short", "")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("GENERAL_001"));
  }
}
