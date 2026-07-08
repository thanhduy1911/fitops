package com.fitops.commons.security;

import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class ProblemDetailAuthenticationEntryPointTest {
  @Mock ProblemDetailResolver problemDetailResolver;
  @Mock HttpServletRequest request;
  @Mock HttpServletResponse response;

  @Test
  void commence_delegatesToProblemDetailResolver() {
    var entryPoint = new ProblemDetailAuthenticationEntryPoint(problemDetailResolver);
    var exception = new BadCredentialsException("Bad Credentials");

    entryPoint.commence(request, response, exception);

    verify(problemDetailResolver).render(request, response, exception);
  }
}
