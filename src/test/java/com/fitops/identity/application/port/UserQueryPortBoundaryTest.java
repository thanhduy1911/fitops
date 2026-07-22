package com.fitops.identity.application.port;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.FitopsApplication;
import com.fitops.identity.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

class UserQueryPortBoundaryTest {
  private static final ApplicationModules MODULES = ApplicationModules.of(FitopsApplication.class);

  private ApplicationModule identity() {
    return MODULES.getModuleByName("identity").orElseThrow();
  }

  @Test
  void identity_declares_the_port_namedInterface() {
    assertThat(identity().getNamedInterfaces().getByName("port")).isPresent();
  }

  @Test
  void port_types_are_exposed() {
    var identity = identity();
    assertThat(identity.isExposed(UserQueryPort.class)).isTrue();
    assertThat(identity.isExposed(UserSummary.class)).isTrue();
  }

  @Test
  void persistence_stays_internal() {
    assertThat(identity().isExposed(UserRepository.class)).isFalse();
  }

  @Test
  void driven_port_stays_internal() {
    assertThat(identity().isExposed(PasswordResetMailer.class)).isFalse();
  }
}
