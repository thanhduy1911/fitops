package com.fitops.commons.exception;

import com.fitops.commons.constants.ErrorCode;

public class ConflictException extends FitOpsException {
  public ConflictException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
