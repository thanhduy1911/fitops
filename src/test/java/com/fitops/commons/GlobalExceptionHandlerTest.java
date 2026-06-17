package com.fitops.commons;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fitops.commons.constants.ErrorCode;
import com.fitops.commons.constants.MDCConstant;
import com.fitops.commons.exception.ConflictException;
import com.fitops.commons.exception.FitOpsException;
import com.fitops.commons.exception.GlobalExceptionHandler;
import com.fitops.commons.exception.RateLimitExceededException;
import com.fitops.commons.security.JwtService;
import com.fitops.commons.security.RateLimitFilter;
import com.fitops.commons.security.RateLimitProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.*;

@WebMvcTest(
    controllers = GlobalExceptionHandlerTest.TestController.class,
    excludeAutoConfiguration = {
      SecurityAutoConfiguration.class,
      UserDetailsServiceAutoConfiguration.class
    })
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
public class GlobalExceptionHandlerTest {
  private static final String REQUEST_ID = "request-id-1";

  @Autowired MockMvc mockMvc;
  @MockitoBean JwtService jwtService;
  @MockitoBean RateLimitProperties rateLimitProperties;
  @MockitoBean RateLimitFilter rateLimitFilter;

  @BeforeEach
  void setMdc() {
    MDC.put(MDCConstant.REQUEST_ID.getKey(), REQUEST_ID);
  }

  @AfterEach
  void clearMdc() {
    MDC.remove(MDCConstant.REQUEST_ID.getKey());
  }

  @Test
  void validationFailure_returns400_withViolationsAndRequestId() throws Exception {
    mockMvc
        .perform(
            post("/test/echo").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Validation error"))
        .andExpect(jsonPath("$.errorCode").value("GENERAL_001"))
        .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
        .andExpect(jsonPath("$.violations[0].field").value("name"))
        .andExpect(jsonPath("$.violations[0].message").exists());
  }

  @Test
  void malformedJson_returns400_enrichedWithGeneral001() throws Exception {
    mockMvc
        .perform(post("/test/echo").contentType(MediaType.APPLICATION_JSON).content("{not-json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("GENERAL_001"))
        .andExpect(jsonPath("$.requestId").value(REQUEST_ID));
  }

  @Test
  void fitOpsException_routesToErrorCodeStatus() throws Exception {
    mockMvc
        .perform(get("/test/throw-fitops"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errorCode").value("GENERAL_002"))
        .andExpect(jsonPath("$.title").value("Unexpected error"))
        .andExpect(jsonPath("$.requestId").value(REQUEST_ID));
  }

  @Test
  void uncaughtRuntimeException_returns500_withGeneral002() throws Exception {
    mockMvc
        .perform(get("/test/throw-runtime"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errorCode").value("GENERAL_002"))
        .andExpect(jsonPath("$.requestId").value(REQUEST_ID));
  }

  @Test
  void conflictException_returns409_withErrorCode() throws Exception {
    mockMvc
        .perform(get("/test/throw-conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("AUTH_004"))
        .andExpect(jsonPath("$.title").value("Email already registered"))
        .andExpect(jsonPath("$.requestId").value(REQUEST_ID));
  }

  @Test
  void rateLimitExceeded_returns429_withAuth006AndRetryAfter() throws Exception {
    mockMvc
        .perform(get("/test/throw-ratelimit"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "12"))
        .andExpect(jsonPath("$.errorCode").value("AUTH_006"))
        .andExpect(jsonPath("$.title").value("Rate limit exceeded"))
        .andExpect(jsonPath("$.requestId").value(REQUEST_ID));
  }

  @RestController
  @RequestMapping("/test")
  static class TestController {

    @PostMapping("/echo")
    Echo echo(@Valid @RequestBody Echo body) {
      return body;
    }

    @GetMapping("/throw-fitops")
    void throwFitOps() {
      throw new TestFitOpsException();
    }

    @GetMapping("/throw-runtime")
    void throwRuntime() {
      throw new RuntimeException("Something went wrong");
    }

    @GetMapping("/throw-conflict")
    void throwConflict() {
      throw new ConflictException(ErrorCode.AUTH_004, "Email already registered");
    }

    @GetMapping("/throw-ratelimit")
    void throwRateLimit() {
      throw new RateLimitExceededException(12);
    }
  }

  record Echo(@NotBlank String name) {}

  static class TestFitOpsException extends FitOpsException {
    TestFitOpsException() {
      super(ErrorCode.GENERAL_002, "Something went wrong");
    }
  }
}
