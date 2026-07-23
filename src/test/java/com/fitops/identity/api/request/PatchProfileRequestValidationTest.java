package com.fitops.identity.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.commons.api.PatchValue;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class PatchProfileRequestValidationTest {
  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  private boolean violates(PatchProfileRequest patchProfileRequest, String field) {
    return validator.validate(patchProfileRequest).stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals(field));
  }

  private static PatchProfileRequest displayName(PatchValue<String> v) {
    return new PatchProfileRequest(v, PatchValue.undefined(), PatchValue.undefined());
  }

  private static PatchProfileRequest language(PatchValue<String> v) {
    return new PatchProfileRequest(PatchValue.undefined(), v, PatchValue.undefined());
  }

  private static PatchProfileRequest avatarUrl(PatchValue<String> v) {
    return new PatchProfileRequest(PatchValue.undefined(), PatchValue.undefined(), v);
  }

  // displayName: @Size unwraps; undefined & explicit-null both pass (both mean "no value to size")
  @Test
  void displayName_over255_rejected() {
    assertThat(violates(displayName(PatchValue.of("a".repeat(256))), "displayName")).isTrue();
  }

  @Test
  void displayName_explicitNull_accepted_clearable() {
    assertThat(violates(displayName(PatchValue.ofNull()), "displayName")).isFalse();
  }

  @Test
  void displayName_undefined_accepted() {
    assertThat(violates(displayName(PatchValue.undefined()), "displayName")).isFalse();
  }

  // language: the heart of the ticket — undefined OK, explicit-null REJECTED, value allowlisted
  @Test
  void language_valid_accepted() {
    assertThat(violates(language(PatchValue.of("vi")), "language")).isFalse();
  }

  @Test
  void language_uppercase_accepted_caseInsensitive() {
    assertThat(violates(language(PatchValue.of("EN")), "language")).isFalse();
  }

  @Test
  void language_notInAllowlist_rejected() {
    assertThat(violates(language(PatchValue.of("de")), "language")).isTrue();
  }

  @Test
  void language_explicitNull_rejected() { // @NotExplicitlyNull(Skip)
    assertThat(violates(language(PatchValue.ofNull()), "language")).isTrue();
  }

  @Test
  void language_undefined_accepted() { // omitted is allowed
    assertThat(violates(language(PatchValue.undefined()), "language")).isFalse();
  }

  // avatarUrl: https-only, ≤500; undefined & explicit-null both pass
  @Test
  void avatarUrl_http_rejected() {
    assertThat(violates(avatarUrl(PatchValue.of("http://cdn/x.png")), "avatarUrl")).isTrue();
  }

  @Test
  void avatarUrl_https_accepted() {
    assertThat(violates(avatarUrl(PatchValue.of("https://cdn/x.png")), "avatarUrl")).isFalse();
  }

  @Test
  void avatarUrl_explicitNull_accepted_clearable() {
    assertThat(violates(avatarUrl(PatchValue.ofNull()), "avatarUrl")).isFalse();
  }

  @Test
  void avatarUrl_over500_rejected() {
    assertThat(violates(avatarUrl(PatchValue.of("https://cdn/" + "a".repeat(500))), "avatarUrl"))
        .isTrue();
  }

  // the mandatory compact-constructor safety net: a directly-built Java-null becomes undefined,
  // so its constraints are NOT silently skipped — they validate as a legitimately-absent field
  @Test
  void javaNullFields_normalizedToUndefined_pass() {
    assertThat(validator.validate(new PatchProfileRequest(null, PatchValue.of("vi"), null)))
        .isEmpty();
  }
}
