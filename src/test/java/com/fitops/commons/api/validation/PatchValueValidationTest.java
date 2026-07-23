package com.fitops.commons.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.commons.api.PatchValue;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Size;
import jakarta.validation.valueextraction.Unwrapping;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PatchValueValidationTest {
  record Probe(@Size(max = 3) PatchValue<String> name) {}

  record LanguageProbe(
      @NotExplicitlyNull(payload = Unwrapping.Skip.class) @Size(max = 2)
          PatchValue<String> language) {}

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  @Test
  void sizeConstraintAppliesToTheWrapperedValue() {
    assertThat(validator.validate(new Probe(PatchValue.of("abcd")))).hasSize(1);
    assertThat(validator.validate(new Probe(PatchValue.of("abc")))).isEmpty();
  }

  @Test
  void violationPathNamesTheFieldNotTheContainer() {
    var violation = validator.validate(new Probe(PatchValue.of("abcd"))).iterator().next();
    assertThat(violation.getPropertyPath()).hasToString("name");
  }

  @Test
  void undefinedAndExplicitNullPassAValueConstraint() {
    assertThat(validator.validate(new Probe(PatchValue.undefined()))).isEmpty();
    assertThat(validator.validate(new Probe(PatchValue.ofNull()))).isEmpty();
  }

  @Test
  void explicitNullIsRejected() {
    assertThat(validator.validate(new LanguageProbe(PatchValue.ofNull()))).hasSize(1);
  }

  @Test
  void undefinedIsAccepted() {
    assertThat(validator.validate(new LanguageProbe(PatchValue.undefined()))).isEmpty();
  }

  @Test
  void valueIsAccepted() {
    assertThat(validator.validate(new LanguageProbe(PatchValue.of("vi")))).isEmpty();
  }

  @Test
  void theSkippedConstraintCoexistsWithAnUnwrappedOne() {
    var violations = validator.validate(new LanguageProbe(PatchValue.of("vietnamese")));

    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getPropertyPath()).hasToString("language");
  }
}
