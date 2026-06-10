package com.fitops.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.commons.security.OpaqueTokens;
import com.fitops.identity.domain.entity.PasswordResetToken;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
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
class PasswordResetTokenRepositoryTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgreSQLContainer =
      new PostgreSQLContainer("postgres:18-alpine").withReuse(true);

  @Autowired private PasswordResetTokenRepository repository;
  @Autowired private TestEntityManager entityManager;

  private PasswordResetToken active(UUID userId, String raw) {
    return PasswordResetToken.builder()
        .userId(userId)
        .tokenHash(OpaqueTokens.sha256Hex(raw))
        .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1))
        .build();
  }

  @Test
  void findByTokenHash_returnsSavedToken() {
    var userId = UUID.randomUUID();
    var token = active(userId, "raw-1");
    entityManager.persistAndFlush(token);
    entityManager.clear();

    var found = repository.findByTokenHash(OpaqueTokens.sha256Hex("raw-1"));

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(token.getId());
  }

  @Test
  void consumeAllActiveByUserId_consumesOnlyTargetUsersActiveTokens() {
    var userA = UUID.randomUUID();
    var userB = UUID.randomUUID();
    var now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);

    var aActive = active(userA, "a-active");
    var aAlready = active(userA, "a-consumed");
    aAlready.setConsumedAt(now.minusMinutes(10));
    var bActive = active(userB, "b-active");
    entityManager.persist(aActive);
    entityManager.persist(aAlready);
    entityManager.persist(bActive);
    entityManager.flush();

    int updated = repository.consumeAllActiveByUserId(userA, now);
    entityManager.clear();

    assertThat(updated).isEqualTo(1);
    assertThat(
            Objects.requireNonNull(entityManager.find(PasswordResetToken.class, aActive.getId()))
                .getConsumedAt())
        .isNotNull();
    assertThat(
            Objects.requireNonNull(entityManager.find(PasswordResetToken.class, aAlready.getId()))
                .getConsumedAt())
        .isEqualTo(now.minusMinutes(10));
    assertThat(
            Objects.requireNonNull(entityManager.find(PasswordResetToken.class, bActive.getId()))
                .getConsumedAt())
        .isNull();
  }
}
