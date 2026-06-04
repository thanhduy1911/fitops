package com.fitops.identity.api.validation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Validates that a string's UTF-8 byte length does not exceed {@code value} (e.g. BCrypt's 72-byte
 * cap).
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface MaxUtf8Bytes {
  String message() default "must not exceed {value} UTF-8 bytes";

  int value();

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
