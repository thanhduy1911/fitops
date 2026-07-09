package com.fitops.identity.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;
import java.util.Set;

public class SupportedLanguageValidator implements ConstraintValidator<SupportedLanguage, String> {
  private static final Set<String> SUPPORTED = Set.of("vi", "en");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) return true; // @NotBlank's job

    return SUPPORTED.contains(value.toLowerCase(Locale.ROOT)); // case-insensitive
  }
}
