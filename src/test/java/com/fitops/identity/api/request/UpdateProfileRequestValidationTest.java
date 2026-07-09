package com.fitops.identity.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class UpdateProfileRequestValidationTest {
  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  private boolean violates(UpdateProfileRequest request, String field) {
    return validator.validate(request).stream()
        .anyMatch(violation -> violation.getPropertyPath().toString().equals(field));
  }

  @Test
  void language_lowercase_accepted() {
    assertThat(violates(new UpdateProfileRequest("Bob", "vi", null), "language")).isFalse();
  }

  @Test
  void language_uppercase_accepted_caseInsensitive() {
    assertThat(violates(new UpdateProfileRequest("Bob", "EN", null), "language")).isFalse();
  }

  @Test
  void language_notInAllowlist_rejected() {
    assertThat(violates(new UpdateProfileRequest("Bob", "de", null), "language")).isTrue();
  }

  @Test
  void language_blank_rejected() {
    assertThat(violates(new UpdateProfileRequest("Bob", "  ", null), "language")).isTrue();
  }

  @Test
  void avatarUrl_http_rejected() {
    assertThat(violates(new UpdateProfileRequest("Bob", "vi", "http://cdn/x.png"), "avatarUrl"))
        .isTrue();
  }

  @Test
  void avatarUrl_https_accepted() {
    assertThat(violates(new UpdateProfileRequest("Bob", "vi", "https://cdn/x.png"), "avatarUrl"))
        .isFalse();
  }

  @Test
  void avatarUrl_null_accepted_clearable() {
    assertThat(violates(new UpdateProfileRequest("Bob", "vi", null), "avatarUrl")).isFalse();
  }

  @Test
  void avatarUrl_over500_rejected() {
    String longUrl = "https://cdn/" + "a".repeat(500);
    assertThat(violates(new UpdateProfileRequest("Bob", "vi", longUrl), "avatarUrl")).isTrue();
  }

  @Test
  void displayName_over255_rejected() {
    assertThat(violates(new UpdateProfileRequest("a".repeat(256), "vi", null), "displayName"))
        .isTrue();
  }

  @Test
  void displayName_null_accepted() {
    assertThat(violates(new UpdateProfileRequest(null, "vi", null), "displayName")).isFalse();
  }
}
