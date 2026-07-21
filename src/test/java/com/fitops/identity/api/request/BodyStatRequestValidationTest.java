package com.fitops.identity.api.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.identity.domain.valueobject.ActivityLevel;
import com.fitops.identity.domain.valueobject.Gender;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

public class BodyStatRequestValidationTest {
  private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private final Validator validator = factory.getValidator();

  private static final BigDecimal VALID_HEIGHT = new BigDecimal("167.00");
  private static final BigDecimal VALID_WEIGHT = new BigDecimal("75.00");
  private static final LocalDate VALID_DOB = LocalDate.now(ZoneOffset.UTC).minusYears(26);

  private boolean violates(BodyStatRequest request, String field) {
    return validator.validate(request).stream()
        .anyMatch(value -> value.getPropertyPath().toString().equals(field));
  }

  private BodyStatRequest withHeight(BigDecimal height) {
    return new BodyStatRequest(
        height, VALID_WEIGHT, VALID_DOB, Gender.MALE, ActivityLevel.VERY_ACTIVE);
  }

  private BodyStatRequest withWeight(BigDecimal weight) {
    return new BodyStatRequest(
        VALID_HEIGHT, weight, VALID_DOB, Gender.MALE, ActivityLevel.VERY_ACTIVE);
  }

  private BodyStatRequest withDob(LocalDate dob) {
    return new BodyStatRequest(
        VALID_HEIGHT, VALID_WEIGHT, dob, Gender.MALE, ActivityLevel.VERY_ACTIVE);
  }

  @Test
  void height_atLowerBound50_accepted() {
    assertThat(violates(withHeight(new BigDecimal("50.00")), "heightCm")).isFalse();
  }

  @Test
  void height_atUpperBound300_accepted() {
    assertThat(violates(withHeight(new BigDecimal("300.00")), "heightCm")).isFalse();
  }

  @Test
  void height_below50_rejected() {
    assertThat(violates(withHeight(new BigDecimal("49.99")), "heightCm")).isTrue();
  }

  @Test
  void height_above300_rejected() {
    assertThat(violates(withHeight(new BigDecimal("300.01")), "heightCm")).isTrue();
  }

  @Test
  void height_threeDecimals_rejected() {
    assertThat(violates(withHeight(new BigDecimal("167.123")), "heightCm")).isTrue();
  }

  @Test
  void height_null_rejected() {
    assertThat(violates(withHeight(null), "heightCm")).isTrue();
  }

  @Test
  void weight_zero_rejected() {
    assertThat(violates(withWeight(BigDecimal.ZERO), "weightKg")).isTrue();
  }

  @Test
  void weight_atUpperBound500_accepted() {
    assertThat(violates(withWeight(new BigDecimal("500.00")), "weightKg")).isFalse();
  }

  @Test
  void weight_above500_rejected() {
    assertThat(violates(withWeight(new BigDecimal("500.01")), "weightKg")).isTrue();
  }

  @Test
  void weight_null_rejected() {
    assertThat(violates(withWeight(null), "weightKg")).isTrue();
  }

  @Test
  void age_exactly13_accepted() {
    assertThat(violates(withDob(LocalDate.now(ZoneOffset.UTC).minusYears(13)), "dateOfBirth"))
        .isFalse();
  }

  @Test
  void age_just_under13_rejected() {
    assertThat(
            violates(
                withDob(LocalDate.now(ZoneOffset.UTC).minusYears(13).plusDays(1)), "dateOfBirth"))
        .isTrue();
  }

  @Test
  void age_exactly120_accepted() {
    assertThat(violates(withDob(LocalDate.now(ZoneOffset.UTC).minusYears(120)), "dateOfBirth"))
        .isFalse();
  }

  @Test
  void age_over120_rejected() {
    assertThat(
            violates(
                withDob(LocalDate.now(ZoneOffset.UTC).minusYears(124).minusDays(1)), "dateOfBirth"))
        .isTrue();
  }

  @Test
  void dateOfBirth_inFuture_rejected() {
    assertThat(violates(withDob(LocalDate.now(ZoneOffset.UTC).plusDays(1)), "dateOfBirth"))
        .isTrue();
  }

  @Test
  void dateOfBirth_null_rejected() {
    assertThat(violates(withDob(null), "dateOfBirth")).isTrue();
  }
}
