package com.fitops.commons.api.jackson;

import com.fitops.commons.api.PatchValue;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.ReferenceTypeDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;

public class PatchValueDeserializer extends ReferenceTypeDeserializer<PatchValue<?>> {
  public PatchValueDeserializer(
      JavaType fullType, TypeDeserializer typeDeserializer, ValueDeserializer<?> deserializer) {
    super(fullType, null, typeDeserializer, deserializer);
  }

  @Override
  public PatchValueDeserializer withResolved(
      TypeDeserializer typeDeserializer, ValueDeserializer<?> valueDeserializer) {
    return new PatchValueDeserializer(_fullType, typeDeserializer, valueDeserializer);
  }

  /** The key was absent from the JSON object. */
  @Override
  public PatchValue<?> getAbsentValue(DeserializationContext context) {
    return PatchValue.undefined();
  }

  /** The key was present with a null value. */
  @Override
  public PatchValue<?> getNullValue(DeserializationContext context) {
    return PatchValue.ofNull();
  }

  @Override
  public Object getEmptyValue(DeserializationContext context) {
    return getNullValue(context);
  }

  @Override
  public PatchValue<?> referenceValue(Object contents) {
    return PatchValue.ofNullable(contents);
  }

  @Override
  public PatchValue<?> updateReference(PatchValue<?> reference, Object contents) {
    return PatchValue.ofNullable(contents);
  }

  @Override
  public Object getReferenced(PatchValue<?> reference) {
    return reference.value();
  }
}
