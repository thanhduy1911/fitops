package com.fitops.commons.api.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.commons.api.PatchValue;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The tripwire for the whole ticket. The reference-type SPI this relies on is not covered by
 * Jackson's public-API compatibility guarantee, so a Jackson 3.x minor upgrade can break it at
 * runtime with no compile error. This test must run in CI.
 */
class PatchValueDeserializationTest {
  private final JsonMapper mapper = JsonMapper.builder().addModule(new PatchValueModule()).build();

  @Test
  void absentKeyDeserializesToUndefined() {
    Probe probe = mapper.readValue("{}", Probe.class);

    assertThat(probe.displayName()).isEqualTo(PatchValue.undefined());
    assertThat(probe.displayName().isPresent()).isFalse();
  }

  @Test
  void explicitNullDeserializesToNullState() {
    Probe probe = mapper.readValue("{\"displayName\":null}", Probe.class);

    assertThat(probe.displayName()).isEqualTo(PatchValue.ofNull());
    assertThat(probe.displayName().isNull()).isTrue();
  }

  @Test
  void valueDeserializesToValueState() {
    Probe probe = mapper.readValue("{\"displayName\":\"Jane\"}", Probe.class);

    assertThat(probe.displayName()).isEqualTo(PatchValue.of("Jane"));
  }

  @Test
  void absentAndExplicitNullAreNotTheSame() {
    Probe absent = mapper.readValue("{}", Probe.class);
    Probe explicitNull = mapper.readValue("{\"displayName\":null}", Probe.class);

    assertThat(absent.displayName()).isNotEqualTo(explicitNull.displayName());
  }

  @Test
  void contentTypeIsResolvedFromTheTypeParameter() {
    Probe probe = mapper.readValue("{\"age\":42}", Probe.class);

    assertThat(probe.age().get()).isEqualTo(42);
  }

  @Test
  void oneAbsentFieldDoesNotDisturbAPresentOne() {
    Probe probe = mapper.readValue("{\"displayName\":\"Jane\"}", Probe.class);

    assertThat(probe.displayName().hasValue()).isTrue();
    assertThat(probe.age().isPresent()).isFalse();
  }

  record Probe(PatchValue<String> displayName, PatchValue<Integer> age) {}
}
