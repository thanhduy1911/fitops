package com.fitops.commons.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PatchValueTest {
  @Test
  void undefinedAndExplicitNullAreDistinct() {
    assertThat(PatchValue.undefined()).isNotEqualTo(PatchValue.ofNull());
  }

  @Test
  void undefinedIsNeitherPresentNorNull() {
    PatchValue<String> undefined = PatchValue.undefined();

    assertThat(undefined.isPresent()).isFalse();
    assertThat(undefined.isNull()).isFalse();
    assertThat(undefined.hasValue()).isFalse();
  }

  @Test
  void explicitNullIsPresentAndNull() {
    PatchValue<String> explicitNull = PatchValue.ofNull();

    assertThat(explicitNull.isPresent()).isTrue();
    assertThat(explicitNull.isNull()).isTrue();
    assertThat(explicitNull.hasValue()).isFalse();
  }

  @Test
  void valueIsPresentAndNotNull() {
    PatchValue<String> value = PatchValue.of("Jane");

    assertThat(value.isPresent()).isTrue();
    assertThat(value.isNull()).isFalse();
    assertThat(value.hasValue()).isTrue();
    assertThat(value.get()).isEqualTo("Jane");
  }

  @Test
  void ofNullableCollapsesNullToExplicitNull() {
    assertThat(PatchValue.ofNullable(null)).isEqualTo(PatchValue.ofNull());
    assertThat(PatchValue.ofNullable("Jane")).isEqualTo(PatchValue.of("Jane"));
  }

  @Test
  void ifPresentRunsForExplicitNullAndPassesNull() {
    var applied = new ArrayList<String>();

    PatchValue.<String>ofNull().ifPresent(applied::add);

    assertThat(applied).containsExactly((String) null);
  }

  @Test
  void ifPresentDoesNotRunForUndefined() {
    List<String> applied = new ArrayList<>();

    PatchValue.<String>undefined().ifPresent(applied::add);

    assertThat(applied).isEmpty();
  }

  @Test
  void orElseReturnsFallbackForUndefinedAndNullForExplicitNull() {
    assertThat(PatchValue.<String>undefined().orElse("fallback")).isEqualTo("fallback");
    assertThat(PatchValue.<String>ofNull().orElse("fallback")).isNull();
  }

  @Test
  void getThrowsWhenUndefined() {
    assertThatThrownBy(() -> PatchValue.undefined().get())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void canonicalConstructorRejectsTheImpossibleState() {
    assertThatThrownBy(() -> new PatchValue<>(false, "value"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
