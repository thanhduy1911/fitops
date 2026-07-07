package com.fitops.identity.api.controller;

import com.fitops.identity.api.RefreshTokenCookies;
import com.fitops.identity.api.request.ForgotPasswordRequest;
import com.fitops.identity.api.request.LoginRequest;
import com.fitops.identity.api.request.RegisterRequest;
import com.fitops.identity.api.request.ResetPasswordRequest;
import com.fitops.identity.api.response.AuthResponse;
import com.fitops.identity.application.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService authService;
  private final RefreshTokenCookies refreshTokenCookies;

  public AuthController(AuthService authService, RefreshTokenCookies refreshTokenCookies) {
    this.authService = authService;
    this.refreshTokenCookies = refreshTokenCookies;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return AuthResponse.from(authService.register(request));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    var result = authService.login(request);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE, refreshTokenCookies.issue(result.rawRefreshToken()).toString())
        .body(AuthResponse.from(result.accessToken()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      @CookieValue(name = "${fitops.security.refresh-token.cookie-name}", required = false)
          String refreshToken) {
    var result = authService.refresh(refreshToken);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE, refreshTokenCookies.issue(result.rawRefreshToken()).toString())
        .body(AuthResponse.from(result.accessToken()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = "${fitops.security.refresh-token.cookie-name}", required = false)
          String refreshToken) {
    authService.logout(refreshToken);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear().toString())
        .build();
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    authService.forgotPassword(request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ResponseEntity.ok().build();
  }
}
