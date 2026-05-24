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
  ;

  private final HttpStatus status;
  private final String title;
}
