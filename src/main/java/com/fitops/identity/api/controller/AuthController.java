package com.fitops.identity.api.controller;

import com.fitops.commons.security.RefreshTokenProperties;
import com.fitops.identity.api.request.LoginRequest;
import com.fitops.identity.api.request.RegisterRequest;
import com.fitops.identity.api.response.AuthResponse;
import com.fitops.identity.application.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService authService;
  private final RefreshTokenProperties refreshTokenProperties;

  public AuthController(AuthService authService, RefreshTokenProperties refreshTokenProperties) {
    this.authService = authService;
    this.refreshTokenProperties = refreshTokenProperties;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    var result = authService.login(request);
    var cookie =
        ResponseCookie.from(refreshTokenProperties.cookieName(), result.rawRefreshToken())
            .httpOnly(true)
            .secure(refreshTokenProperties.secure())
            .sameSite("Strict")
            .path(refreshTokenProperties.cookiePath())
            .maxAge(refreshTokenProperties.ttl())
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new AuthResponse(result.accessToken(), "Bear", result.expiresIn()));
  }
}
