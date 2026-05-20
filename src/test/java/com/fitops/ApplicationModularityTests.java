package com.fitops;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModularityTests {
  ApplicationModules modules = ApplicationModules.of(FitopsApplication.class);

  @Test
  void verifiesModularStructure() {
    modules.verify();
  }
}
