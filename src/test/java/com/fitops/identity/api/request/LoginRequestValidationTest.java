package com.fitops.identity.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class LoginRequestValidationTest {
  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  @Test
  void validRequest_passes() {
    var violations = validator.validate(new LoginRequest("joe@fitops.com", "password123"));
    assertThat(violations).isEmpty();
  }

  @Test
  void blankEmail_isRejected() {
    var violations = validator.validate(new LoginRequest("", "password123"));
    assertThat(violations)
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("email"));
  }

  @Test
  void malformedEmail_isRejected() {
    var violations = validator.validate(new LoginRequest("not-an-email", "password"));
    assertThat(violations)
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("email"));
  }

  @Test
  void blankPassword_isRejected() {
    var violations = validator.validate(new LoginRequest("john.doe@fitops.com", ""));
    assertThat(violations)
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
  }

  @Test
  void password_over72Utf8Bytes_isRejected() {
    var multibyte = "é".repeat(40); // less than 255 chars but 80 bytes
    var violations = validator.validate(new LoginRequest("john.doe@fitops.com", multibyte));
    assertThat(violations)
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
  }

  // TODO: Enhance password to accept min 8 chars, currently is fine for MVP
  @Test
  void shortPassword_isAccepted_noPolicyLeak() {
    var violations = validator.validate(new LoginRequest("john.doe@fitops.com", "x"));
    assertThat(violations).isEmpty();
  }
}
