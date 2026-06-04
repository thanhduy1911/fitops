package com.fitops.identity.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class RegisterRequestValidationTest {
  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  @Test
  void password_within72CharsButOver72Utf8Bytes_isRejected() {
    String multibyte = "é".repeat(40); // 'é' = 2 bytes -> 40 chars (<=72), 80 bytes (>72)
    var violations =
        validator.validate(new RegisterRequest("a@b.com", "valid_user", multibyte, "X"));
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
  }

  @Test
  void password_ascii72Bytes_isAccepted() {
    String pw = "a".repeat(72); // 72 chars == 72 bytes
    var violations = validator.validate(new RegisterRequest("a@b.com", "valid_user", pw, "X"));
    assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("password"));
  }
}
