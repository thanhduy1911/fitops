package com.fitops.commons.security;

import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
public class ProblemDetailAccessDeniedHandlerTest {
  @Mock ProblemDetailResolver problemDetailResolver;
  @Mock HttpServletRequest request;
  @Mock HttpServletResponse response;

  @Test
  void handle_delegates_to_resolver() {
    var handler = new ProblemDetailAccessDeniedHandler(problemDetailResolver);
    var exception = new AccessDeniedException("denied");
    handler.handle(request, response, exception);
    verify(problemDetailResolver).render(request, response, exception);
  }
}
