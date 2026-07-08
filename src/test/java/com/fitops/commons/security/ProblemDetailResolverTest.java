package com.fitops.commons.security;

import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerExceptionResolver;

@ExtendWith(MockitoExtension.class)
class ProblemDetailResolverTest {
  @Mock HandlerExceptionResolver handlerExceptionResolver;
  @Mock HttpServletRequest request;
  @Mock HttpServletResponse response;

  @Test
  void render_forwardsExceptionToHandlerResolverWithNullHandler() {
    var resolver = new ProblemDetailResolver(handlerExceptionResolver);
    var exception = new RuntimeException("Something went wrong");
    resolver.render(request, response, exception);
    verify(handlerExceptionResolver).resolveException(request, response, null, exception);
  }
}
