package com.fitops.commons.api.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.commons.api.PatchValue;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

class PatchValueModelConverterTest {
  record Probe(PatchValue<String> displayName) {}

  @Test
  void patchValueIsPublishedAsItsWrappedType() {
    var converters = new ModelConverters();
    converters.addConverter(new PatchValueModelConverter());

    Schema<?> schema = converters.readAllAsResolvedSchema(Probe.class).schema;
    var displayName = schema.getProperties().get("displayName");

    assertThat(displayName.getType()).isEqualTo("string");
  }

  @Test
  void theWrapperFieldsAreNotLeakedIntoTheSchema() {
    ModelConverters converters = new ModelConverters();
    converters.addConverter(new PatchValueModelConverter());

    Schema<?> schema = converters.readAllAsResolvedSchema(Probe.class).schema;
    Schema<?> displayName = schema.getProperties().get("displayName");

    assertThat(displayName.getProperties()).isNullOrEmpty();
  }
}
