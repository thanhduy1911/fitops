package com.fitops.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
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
class UserProfileIntegrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:18-alpine").withReuse(true);

  @Autowired MockMvc mockMvc;

  private String registerAndGetToken(String email, String username) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","username":"%s","password":"password123","displayName":"Joe Doe"}
                        """
                            .formatted(email, username)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.accessToken");
  }

  @Test
  void getMe_authenticated_returnsProfile() throws Exception {
    String token = registerAndGetToken("joe.doe@fitops.com", "joe.doe123");
    mockMvc
        .perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("joe.doe@fitops.com"))
        .andExpect(jsonPath("$.username").value("joe.doe123"))
        .andExpect(jsonPath("$.displayName").value("Joe Doe"))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.createdAt").isNotEmpty())
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void getMe_unauthenticated_returns401Auth001() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_001"));
  }

  @Test
  void putMe_updatesThenClearsOmittedAvatar() throws Exception {
    String token = registerAndGetToken("jane@fitops.com", "jane");

    mockMvc
        .perform(
            put("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"displayName":"Jane Doe","language":"en","avatarUrl":"https://cdn/jane.png"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Jane Doe"))
        .andExpect(jsonPath("$.language").value("en"))
        .andExpect(jsonPath("$.avatarUrl").value("https://cdn/jane.png"));

    // Full-replace: omit avatarUrl -> cleared
    mockMvc
        .perform(
            put("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"displayName":"Jane","language":"vi"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avatarUrl").isEmpty()); // null serialized as null
  }

  @Test
  void putMe_invalidLanguage_returns400General001() throws Exception {
    String token = registerAndGetToken("bob@fitops.com", "bob");

    mockMvc
        .perform(
            put("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"displayName":"Bob","language":"de"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("GENERAL_001"));
  }

  @Test
  void putMe_httpAvatar_returns400General001() throws Exception {
    String token = registerAndGetToken("amy@fitops.com", "amy");

    mockMvc
        .perform(
            put("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"displayName":"Amy","language":"vi","avatarUrl":"http://cdn/amy.png"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("GENERAL_001"));
  }
}
