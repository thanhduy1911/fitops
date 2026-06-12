package com.fitops.commons.exception;

import com.fitops.commons.constants.ErrorCode;
import lombok.Getter;

@Getter
public class RateLimitExceededException extends FitOpsException {
  private final long retryAfterSeconds;

  public RateLimitExceededException(long retryAfterSeconds) {
    super(ErrorCode.AUTH_006, "Rate limit exceeded");
    this.retryAfterSeconds = retryAfterSeconds;
  }
}
