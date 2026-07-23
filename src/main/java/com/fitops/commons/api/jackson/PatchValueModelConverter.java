package com.fitops.commons.api.jackson;

import com.fitops.commons.api.PatchValue;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Iterator;

/**
 * Publishes {@code PatchValue<T>} as {@code T} in the OpenAPI document. Without this, springdoc
 * introspects the wrapper as a bean and documents {@code {present, value}} — a schema describing
 * the wrapper rather than the JSON the endpoint accepts.
 */
public class PatchValueModelConverter implements ModelConverter {
  @Override
  public Schema<?> resolve(
      AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
    var javaType = Json.mapper().constructType(type.getType());
    if (javaType != null && PatchValue.class.isAssignableFrom(javaType.getRawClass())) {
      var inner = javaType.containedTypeOrUnknown(0);
      return context.resolve(
          new AnnotatedType(inner).jsonViewAnnotation(type.getJsonViewAnnotation()));
    }
    return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
  }
}
