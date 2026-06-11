package com.fitops.identity.infrastructure.persistence;

import com.fitops.identity.domain.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      "UPDATE PasswordResetToken t SET t.consumedAt = :now "
          + "WHERE t.userId = :userId AND t.consumedAt IS NULL")
  int consumeAllActiveByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
}
