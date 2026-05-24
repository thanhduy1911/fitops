package com.fitops.commons.exception;

import com.fitops.commons.constants.ErrorCode;

public class NotFoundException extends FitOpsException {
  public NotFoundException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
