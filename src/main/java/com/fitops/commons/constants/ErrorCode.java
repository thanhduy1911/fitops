package com.fitops.commons.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  GENERAL_001(HttpStatus.BAD_REQUEST, "Validation error"),
  GENERAL_002(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error"),
  AUTH_001(HttpStatus.UNAUTHORIZED, "Authentication required"),
  AUTH_003(HttpStatus.UNAUTHORIZED, "Refresh token invalid or revoked"),
  AUTH_004(HttpStatus.CONFLICT, "Email already registered"),
  AUTH_005(HttpStatus.CONFLICT, "Username already taken"),
  AUTH_006(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"),
  AUTH_007(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
  AUTH_009(HttpStatus.BAD_REQUEST, "Password reset token invalid or expired"),
  ;

  private final HttpStatus status;
  private final String title;
}
