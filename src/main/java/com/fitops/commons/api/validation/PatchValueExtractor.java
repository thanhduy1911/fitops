package com.fitops.commons.api.validation;

import com.fitops.commons.api.PatchValue;
import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.UnwrapByDefault;
import jakarta.validation.valueextraction.ValueExtractor;

/**
 * Makes Bean Validation constraints on a {@link PatchValue} field apply to the wrapped value, so a
 * PATCH DTO reads like the PUT DTO it mirrors.
 *
 * <p>{@code @UnwrapByDefault} means <em>every</em> constraint on such a field targets the inner
 * value. A constraint about the container itself, such as {@link NotExplicitlyNull} must opt out
 * with {@code payload = Unwrapping.Skip.class}.
 *
 * <p>Both {@code undefined} and explicit-null extract to {@code null}, which standard constraints
 * treat as valid. That is intended: "the client did not send a value" is never a constraint
 * violation on the value.
 */
@UnwrapByDefault
public class PatchValueExtractor implements ValueExtractor<PatchValue<@ExtractedValue ?>> {

  @Override
  public void extractValues(PatchValue<?> originalValue, ValueReceiver receiver) {
    receiver.value(null, originalValue.orElse(null));
  }
}
