package com.fitops.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.identity.domain.entity.BodyStat;
import com.fitops.identity.domain.valueobject.ActivityLevel;
import com.fitops.identity.domain.valueobject.Gender;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BodyStatRepositoryTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:18-alpine").withReuse(true);

  @Autowired private BodyStatRepository repository;
  @Autowired private TestEntityManager entityManager;

  private BodyStat stat(UUID userId, OffsetDateTime recordedAt) {
    return BodyStat.builder()
        .userId(userId)
        .heightCm(new BigDecimal("175.00"))
        .weightKg(new BigDecimal("70.00"))
        .dateOfBirth(LocalDate.of(1996, 1, 1))
        .gender(Gender.MALE)
        .activityLevel(ActivityLevel.SEDENTARY)
        .recordedAt(recordedAt)
        .build();
  }

  @Test
  void findByUserId_returnsNewestFirst() {
    var userId = UUID.randomUUID();
    var base = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    var oldest = stat(userId, base.minusMinutes(2));
    var middle = stat(userId, base.minusMinutes(1));
    var newest = stat(userId, base);
    entityManager.persist(oldest);
    entityManager.persist(middle);
    entityManager.persist(newest);
    entityManager.flush();
    entityManager.clear();

    var page = repository.findByUserIdOrderByRecordedAtDesc(userId, PageRequest.of(0, 10));

    assertThat(page.getContent())
        .extracting(BodyStat::getId)
        .containsExactly(newest.getId(), middle.getId(), oldest.getId());
  }

  @Test
  void findByUserId_scopesToThatUserOnly() {
    var userA = UUID.randomUUID();
    var userB = UUID.randomUUID();
    var now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    entityManager.persist(stat(userA, now));
    entityManager.persist(stat(userB, now));
    entityManager.flush();
    entityManager.clear();

    var page = repository.findByUserIdOrderByRecordedAtDesc(userA, PageRequest.of(0, 10));

    assertThat(page.getContent()).extracting(BodyStat::getUserId).containsExactly(userA);
  }

  @Test
  void findByUserId_paginates() {
    var userId = UUID.randomUUID();
    var base = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    for (int i = 0; i < 3; i++) {
      entityManager.persist(stat(userId, base.minusMinutes(i)));
    }
    entityManager.flush();
    entityManager.clear();

    var firstPage = repository.findByUserIdOrderByRecordedAtDesc(userId, PageRequest.of(0, 2));

    assertThat(firstPage.getTotalElements()).isEqualTo(3);
    assertThat(firstPage.getContent()).hasSize(2);
    assertThat(firstPage.hasNext()).isTrue();
    assertThat(firstPage.hasPrevious()).isFalse();
  }
}
