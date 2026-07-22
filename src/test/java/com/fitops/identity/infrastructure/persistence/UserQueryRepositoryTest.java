package com.fitops.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.identity.domain.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserQueryRepositoryTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgreSQLContainer =
      new PostgreSQLContainer("postgres:18-alpine").withReuse(true);

  @Autowired private UserRepository repository;
  @Autowired private TestEntityManager entityManager;

  @Test
  void find_active_summary_by_id_returns_the_projection_for_an_active_user() {
    var saved = persist(user("active", true));
    var summary = repository.findActiveSummaryById(saved.getId());

    assertThat(summary).isPresent();
    assertThat(summary.get().id()).isEqualTo(saved.getId());
    assertThat(summary.get().username()).isEqualTo("user-active");
    assertThat(summary.get().displayName()).isEqualTo("Display active");
    assertThat(summary.get().language()).isEqualTo("vi");
  }

  @Test
  void findActiveSummaryById_toleratesNullDisplayName() {
    var user = user("nodisplay", true);
    user.setDisplayName(null);
    var saved = persist(user);

    var summary = repository.findActiveSummaryById(saved.getId());

    assertThat(summary).isPresent();
    assertThat(summary.get().displayName()).isNull();
  }

  @Test
  void findActiveSummaryById_isEmptyForADeactivatedUser() {
    var saved = persist(user("inactive", false));

    assertThat(repository.findActiveSummaryById(saved.getId())).isEmpty();
  }

  @Test
  void findActiveSummaryById_isEmptyForAnUnknownId() {
    assertThat(repository.findActiveSummaryById(UUID.randomUUID())).isEmpty();
  }

  @Test
  void existsActiveById_isTrueOnlyForAnActiveUser() {
    var active = persist(user("exists-active", true));
    var inactive = persist(user("exists-inactive", false));

    assertThat(repository.existsActiveById(active.getId())).isTrue();
    assertThat(repository.existsActiveById(inactive.getId())).isFalse();
    assertThat(repository.existsActiveById(UUID.randomUUID())).isFalse();
  }

  private User user(String suffix, boolean active) {
    return User.builder()
        .email("user-" + suffix + "@fitops.test")
        .username("user-" + suffix)
        .password("$2a$10$notarealhashnotarealhashnotarealhashnotarealhashno")
        .displayName("Display " + suffix)
        .language("vi")
        .isActive(active)
        .build();
  }

  private User persist(User user) {
    entityManager.persist(user);
    entityManager.flush();
    entityManager.clear();
    return user;
  }
}
