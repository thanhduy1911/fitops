package com.fitops.commons.exception;

import com.fitops.commons.constants.ErrorCode;
import java.util.Objects;
import lombok.Getter;

@Getter
public abstract class FitOpsException extends RuntimeException {
  private final ErrorCode errorCode;

  protected FitOpsException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
  }

  protected FitOpsException(ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
  }
}
