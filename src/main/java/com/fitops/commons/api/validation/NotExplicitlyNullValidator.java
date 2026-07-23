package com.fitops.commons.api.validation;

import com.fitops.commons.api.PatchValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotExplicitlyNullValidator
    implements ConstraintValidator<NotExplicitlyNull, PatchValue<?>> {
  @Override
  public boolean isValid(PatchValue<?> value, ConstraintValidatorContext context) {
    return value == null || !value.isNull();
  }
}
