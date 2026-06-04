package com.fitops.identity.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class MaxUtf8BytesValidator implements ConstraintValidator<MaxUtf8Bytes, CharSequence> {
  private int max;

  @Override
  public void initialize(MaxUtf8Bytes constraintAnnotation) {
    this.max = constraintAnnotation.value();
  }

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    if (value == null) {
      return true; // @NotBlank's job
    }

    return value.toString().getBytes(StandardCharsets.UTF_8).length <= this.max;
  }
}
