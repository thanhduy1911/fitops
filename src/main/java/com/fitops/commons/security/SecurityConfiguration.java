package com.fitops.commons.security;

import com.fitops.commons.constants.ServiceHeader;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfiguration {
  private static final String[] PUBLIC_PATHS = {
    "/actuator/health",
    "/v3/api-docs/**",
    "/swagger-ui.html",
    "/swagger-ui/**",
    "/api/v1/auth/**",
    "/error"
  };

  @Bean
  @SuppressWarnings("RedundantThrows")
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthFilter jwtAuthFilter,
      JwtAuthenticationEntryPoint entryPoint,
      CorsConfigurationSource corsConfigurationSource)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_PATHS).permitAll().anyRequest().authenticated())
        .exceptionHandling(exception -> exception.authenticationEntryPoint(entryPoint))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties props) {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(props.allowedOrigins());
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    cfg.setAllowedHeaders(
        List.of("Authorization", "Content-Type", ServiceHeader.REQUEST_ID_HEADER.getHeaderName()));
    cfg.setExposedHeaders(List.of(ServiceHeader.REQUEST_ID_HEADER.getHeaderName()));
    cfg.setAllowCredentials(true);
    cfg.setMaxAge(props.maxAge());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }
}
