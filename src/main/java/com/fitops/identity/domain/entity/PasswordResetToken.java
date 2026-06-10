package com.fitops.identity.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.*;

@Entity
@Table(schema = "identity", name = "password_reset_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(onlyExplicitlyIncluded = true)
public class PasswordResetToken {
  @Id
  @ToString.Include
  @Builder.Default
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id = UUID.randomUUID();

  @Column(name = "user_id", updatable = false, nullable = false)
  private UUID userId;

  @Column(name = "token_hash", updatable = false, nullable = false, unique = true)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private OffsetDateTime expiresAt;

  @Column(name = "consumed_at")
  private OffsetDateTime consumedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void onCreate() {
    this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
