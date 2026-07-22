package com.fitops.identity.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;

public class ValidAgeValidator implements ConstraintValidator<ValidAge, LocalDate> {
  private int min;
  private int max;

  @Override
  public void initialize(ValidAge constraintAnnotation) {
    this.min = constraintAnnotation.min();
    this.max = constraintAnnotation.max();
  }

  @Override
  public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
    if (dateOfBirth == null) return true; // @NotNull's job
    var today = LocalDate.now(ZoneOffset.UTC);
    if (dateOfBirth.isAfter(today)) return false;

    int age = Period.between(dateOfBirth, today).getYears();
    return age >= min && age <= max;
  }
}
