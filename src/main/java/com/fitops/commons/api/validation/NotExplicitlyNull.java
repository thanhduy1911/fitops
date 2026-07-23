package com.fitops.commons.api.validation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The annotated PATCH field may be omitted, but may not be sent as an explicit {@code null}.
 *
 * <p>Declare it with {@code payload = Unwrapping.Skip.class}. Without that, {@link
 * PatchValueExtractor}'s {@code @UnwrapByDefault} hands this constraint the wrapped value instead
 * of the container, and it can no longer tell an absent field from an explicitly null one:
 *
 * <pre>{@code
 * @NotExplicitlyNull(payload = Unwrapping.Skip.class)
 * @SupportedLanguage
 * PatchValue<String> language
 * }</pre>
 */
@Documented
@Constraint(validatedBy = NotExplicitlyNullValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface NotExplicitlyNull {
  String message() default "must not be null when present";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
