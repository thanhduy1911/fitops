package com.fitops.commons.api.jackson;

import com.fitops.commons.api.PatchValue;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.ReferenceType;

public class PatchValueDeserializers extends Deserializers.Base {
  @Override
  public ValueDeserializer<?> findReferenceDeserializer(
      ReferenceType refType,
      DeserializationConfig config,
      BeanDescription.Supplier beanDescRef,
      TypeDeserializer contentTypeDeserializer,
      ValueDeserializer<?> contentDeserializer) {
    if (!refType.hasRawClass(PatchValue.class)) {
      return null;
    }
    return new PatchValueDeserializer(refType, contentTypeDeserializer, contentDeserializer);
  }

  @Override
  public boolean hasDeserializerFor(DeserializationConfig config, Class<?> valueType) {
    return valueType == PatchValue.class;
  }
}
