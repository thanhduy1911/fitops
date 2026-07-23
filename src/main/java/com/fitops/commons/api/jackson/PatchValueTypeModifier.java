package com.fitops.commons.api.jackson;

import com.fitops.commons.api.PatchValue;
import java.lang.reflect.Type;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.ReferenceType;
import tools.jackson.databind.type.TypeBindings;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.type.TypeModifier;

public class PatchValueTypeModifier extends TypeModifier {
  @Override
  public JavaType modifyType(
      JavaType type, Type jdkType, TypeBindings context, TypeFactory typeFactory) {
    if (type.isReferenceType() || type.isContainerType()) {
      return type;
    }
    if (type.getRawClass() != PatchValue.class) {
      return type;
    }
    return ReferenceType.upgradeFrom(type, type.containedTypeOrUnknown(0));
  }
}
