package com.fitops.commons.exception;

import com.fitops.commons.constants.ErrorCode;

public class BadRequestException extends FitOpsException {
  public BadRequestException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
