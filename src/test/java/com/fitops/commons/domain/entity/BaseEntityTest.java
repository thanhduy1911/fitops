package com.fitops.commons.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest(
    properties = {
      "spring.flyway.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class BaseEntityTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestEntityManager entityManager;

  @Test
  void id_isAssignedOnConstruction_beforePersist() {
    var entity = new TestEntity("before-persist");

    assertThat(entity.getId()).isNotNull();
  }

  @Test
  void auditFields_arePopulatedOnFirstPersist() {
    var saved = entityManager.persistAndFlush(new TestEntity("audit-test"));

    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
  }

  @Test
  void updatedAt_changesOnUpdate_createdAt_remainsUnchanged() throws InterruptedException {
    var saved = entityManager.persistAndFlush(new TestEntity("original"));
    var createdAt = saved.getCreatedAt();
    var firstUpdatedAt = saved.getUpdatedAt();

    Thread.sleep(10); // ensure the clock advances before the next flush
    saved.setName("updated");
    entityManager.flush();

    assertThat(saved.getUpdatedAt()).isAfter(firstUpdatedAt);
    assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
  }

  @SuppressWarnings("unused")
  @Entity
  @Table(name = "test_entities")
  static class TestEntity extends BaseEntity {

    @Column(name = "name")
    private String name;

    protected TestEntity() {}

    TestEntity(String name) {
      this.name = name;
    }

    void setName(String name) {
      this.name = name;
    }
  }

}
