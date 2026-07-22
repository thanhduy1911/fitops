package com.fitops.commons.api.jackson;

import tools.jackson.databind.module.SimpleModule;

public class PatchValueModule extends SimpleModule {
  public PatchValueModule() {
    super(PatchValueModule.class.getSimpleName());
  }

  @Override
  public void setupModule(SetupContext context) {
    super.setupModule(context);
    context.addTypeModifier(new PatchValueTypeModifier());
    context.addDeserializers(new PatchValueDeserializers());
  }
}
