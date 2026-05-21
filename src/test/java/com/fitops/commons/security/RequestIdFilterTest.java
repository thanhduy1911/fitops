package com.fitops.commons.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RequestIdFilterTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:18-alpine");

  @LocalServerPort private int port;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
  }

  @Test
  void shouldGenerateRequestIdWhenAbsent() {
    given()
        .when()
        .get("/v3/api-docs")
        .then()
        .statusCode(200)
        .header(RequestIdFilter.REQUEST_ID_HEADER, notNullValue());
  }

  @Test
  void shouldEchoValidRequestId() {
    given()
        .header(RequestIdFilter.REQUEST_ID_HEADER, "request-id-1")
        .when()
        .get("v3/api-docs")
        .then()
        .statusCode(200)
        .header(RequestIdFilter.REQUEST_ID_HEADER, equalTo("request-id-1"));
  }

  @Test
  void shouldRejectInvalidRequestIdAndGenerateFresh() {
    given()
        .header(RequestIdFilter.REQUEST_ID_HEADER, "invalid request id")
        .when()
        .get("/v3/api-docs")
        .then()
        .statusCode(200)
        .header(RequestIdFilter.REQUEST_ID_HEADER, notNullValue())
        .header(RequestIdFilter.REQUEST_ID_HEADER, not(equalTo("invalid request id")));
  }

  @Test
  void shouldSkipRequestIdForHealthEndpoint() {
    given()
        .when()
        .get("/actuator/health")
        .then()
        .statusCode(200)
        .header(RequestIdFilter.REQUEST_ID_HEADER, nullValue());
  }
}
