package com.fitops.commons.exception;

import com.fitops.commons.constants.ErrorCode;
import com.fitops.commons.constants.MDCConstant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String ERROR_CODE = "errorCode";
  private static final String VIOLATIONS = "violations";
  private static final String REQUEST_ID = "requestId";
  private static final String REQUEST_ID_MDC = MDCConstant.REQUEST_ID.getKey();

  @ExceptionHandler(FitOpsException.class)
  public ResponseEntity<ProblemDetail> handleFitOpsException(FitOpsException exception) {
    logger.warn("FitOpsException [{}]: {}", exception.getErrorCode(), exception.getMessage());
    return buildResponse(exception.getErrorCode(), exception.getMessage());
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetail> handleAuthenticationException(
      AuthenticationException exception) {
    logger.debug("Authentication failed: {}", exception.getClass().getSimpleName());
    return buildResponse(ErrorCode.AUTH_001, "Authentication required");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUncaught(Exception exception) {
    logger.error("Unhandled exception", exception);
    return buildResponse(ErrorCode.GENERAL_002, "An unexpected error occurred");
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ProblemDetail> handleRateLimitExceeded(
      RateLimitExceededException exception) {
    logger.warn("Rate limit exceeded: [{}]", exception.getErrorCode());
    var body =
        ProblemDetail.forStatusAndDetail(
            exception.getErrorCode().getStatus(), exception.getMessage());
    body.setTitle(exception.getErrorCode().getTitle());
    body.setProperty(ERROR_CODE, exception.getErrorCode().name());
    body.setProperty(REQUEST_ID, MDC.get(REQUEST_ID_MDC));
    return ResponseEntity.status(exception.getErrorCode().getStatus())
        .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
        .body(body);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {
    List<Violation> violations =
        Stream.concat(
                exception.getBindingResult().getFieldErrors().stream()
                    .map(error -> new Violation(error.getField(), error.getDefaultMessage())),
                exception.getBindingResult().getGlobalErrors().stream()
                    .map(error -> new Violation(error.getObjectName(), error.getDefaultMessage())))
            .toList();
    logger.warn("Validation failed: {} field error(s)", violations.size());
    ProblemDetail body =
        ProblemDetail.forStatusAndDetail(
            ErrorCode.GENERAL_001.getStatus(), "Request body failed validation");
    body.setTitle(ErrorCode.GENERAL_001.getTitle());
    body.setProperty(ERROR_CODE, ErrorCode.GENERAL_001.name());
    body.setProperty(VIOLATIONS, violations);
    return handleExceptionInternal(exception, body, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      @NonNull Exception ex,
      Object body,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode statusCode,
      @NonNull WebRequest request) {
    if (body instanceof ProblemDetail problemDetail) {
      Map<String, Object> props = problemDetail.getProperties();
      if (props == null || !props.containsKey(ERROR_CODE)) {
        ErrorCode fallback =
            statusCode.is5xxServerError() ? ErrorCode.GENERAL_002 : ErrorCode.GENERAL_001;
        problemDetail.setProperty(ERROR_CODE, fallback.name());
      }
      problemDetail.setProperty(REQUEST_ID, MDC.get(MDCConstant.REQUEST_ID.getKey()));
    }
    return super.handleExceptionInternal(ex, body, headers, statusCode, request);
  }

  private ResponseEntity<ProblemDetail> buildResponse(ErrorCode errorCode, String detail) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), detail);
    body.setTitle(errorCode.getTitle());
    body.setProperty(ERROR_CODE, errorCode.name());
    body.setProperty(REQUEST_ID, MDC.get(REQUEST_ID_MDC));
    return ResponseEntity.status(errorCode.getStatus()).body(body);
  }
}
