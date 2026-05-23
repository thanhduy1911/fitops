package com.fitops.commons.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
  PaginatedResultIntegrationTest.TestController.class,
  PaginatedResultIntegrationTest.TestSecurityConfig.class
})
@AutoConfigureTestRestTemplate
class PaginatedResultIntegrationTest {
  @Autowired private TestRestTemplate restTemplate;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void listEndPoint_returnsJson_withCorrectPaginationFields() {
    var json = restTemplate.getForObject("/test/items", String.class);
    var node = objectMapper.readTree(json);

    assertThat(node.isObject()).isTrue();
  }

  @RestController
  static class TestController {
    @GetMapping("/test/items")
    PaginatedResult<String> items() {
      var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 5), 12);
      return PaginatedResult.from(page);
    }
  }

  @TestConfiguration
  static class TestSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) {
      return http.securityMatcher("/test/**")
          .csrf(AbstractHttpConfigurer::disable)
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .build();
    }
  }
}
