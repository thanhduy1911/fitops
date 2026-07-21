package com.fitops.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fitops.identity.domain.entity.BodyStat;
import com.fitops.identity.domain.valueobject.ActivityLevel;
import com.fitops.identity.domain.valueobject.Gender;
import com.fitops.identity.infrastructure.persistence.BodyStatRepository;
import com.fitops.identity.infrastructure.persistence.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
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
class BodyStatIntegrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:18-alpine").withReuse(true);

  @Autowired MockMvc mockMvc;
  @Autowired BodyStatRepository bodyStatRepository;
  @Autowired UserRepository userRepository;

  private static final String VALID_BODY =
      """
      {"heightCm":167.00,"weightKg":75.00,"dateOfBirth":"1999-11-19","gender":"MALE","activityLevel":"VERY_ACTIVE"}
      """;

  private String registerAndGetToken(String email, String username) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","username":"%s","password":"password123","displayName":"Joe"}
                        """
                            .formatted(email, username)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.accessToken");
  }

  private UUID userIdOf(String email) {
    return userRepository.findByEmail(email).orElseThrow().getId();
  }

  private BodyStat seed(UUID userId, OffsetDateTime recordedAt, LocalDate dob) {
    return BodyStat.builder()
        .userId(userId)
        .heightCm(new BigDecimal("167.00"))
        .weightKg(new BigDecimal("75.00"))
        .dateOfBirth(dob)
        .gender(Gender.MALE)
        .activityLevel(ActivityLevel.VERY_ACTIVE)
        .recordedAt(recordedAt)
        .build();
  }

  @Test
  void post_authenticated_creates201() throws Exception {
    String token = registerAndGetToken("john.doe@fitops.com", "johndoe1");

    mockMvc
        .perform(
            post("/api/v1/users/me/body-stats")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.heightCm").isNumber())
        .andExpect(jsonPath("$.weightKg").isNumber())
        .andExpect(jsonPath("$.dateOfBirth").value("1999-11-19"))
        .andExpect(jsonPath("$.gender").value("MALE"))
        .andExpect(jsonPath("$.activityLevel").value("VERY_ACTIVE"))
        .andExpect(jsonPath("$.recordedAt").isNotEmpty());
  }

  @Test
  void get_returnsHistoryPaginated1BasedDesc() throws Exception {
    String token = registerAndGetToken("john.doe@fitops.com", "johndoe1");
    UUID uid = userIdOf("john.doe@fitops.com");
    var base = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    bodyStatRepository.save(seed(uid, base.minusMinutes(2), LocalDate.of(1990, 1, 1))); // oldest
    bodyStatRepository.save(seed(uid, base.minusMinutes(1), LocalDate.of(1995, 1, 1)));
    bodyStatRepository.save(seed(uid, base, LocalDate.of(2000, 1, 1))); // newest

    // page 1
    mockMvc
        .perform(
            get("/api/v1/users/me/body-stats?page=1&size=2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItems").value(3))
        .andExpect(jsonPath("$.pageNumber").value(1))
        .andExpect(jsonPath("$.pageSize").value(2))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andExpect(jsonPath("$.hasPrevious").value(false))
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].dateOfBirth").value("2000-01-01")) // newest first
        .andExpect(jsonPath("$.items[1].dateOfBirth").value("1995-01-01"));

    // page 2
    mockMvc
        .perform(
            get("/api/v1/users/me/body-stats?page=2&size=2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pageNumber").value(2))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.hasPrevious").value(true))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].dateOfBirth").value("1990-01-01")); // oldest last
  }

  @Test
  void post_weightOverMax_returns400General001() throws Exception {
    String token = registerAndGetToken("john.doe@fitops.com", "johndoe1");
    mockMvc
        .perform(
            post("/api/v1/users/me/body-stats")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"heightCm":167.00,"weightKg":600,"dateOfBirth":"1999-11-19","gender":"MALE","activityLevel":"VERY_ACTIVE"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("GENERAL_001"));
  }

  @Test
  void post_unauthenticated_returns401Auth001() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/users/me/body-stats")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_001"));
  }

  @Test
  void get_unauthenticated_returns401Auth001() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me/body-stats"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_001"));
  }
}
