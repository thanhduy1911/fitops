package com.fitops.commons.exception;

import com.fitops.commons.constants.ErrorCode;

public class UnauthorizedException extends FitOpsException {
  public UnauthorizedException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
