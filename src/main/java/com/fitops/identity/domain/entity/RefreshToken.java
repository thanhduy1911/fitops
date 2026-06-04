package com.fitops.identity.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.*;

@Entity
@Table(schema = "identity", name = "refresh_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(onlyExplicitlyIncluded = true)
public class RefreshToken {
  @Id
  @ToString.Include
  @Builder.Default
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id = UUID.randomUUID();

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false, unique = true, updatable = false)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private OffsetDateTime expiresAt;

  @Builder.Default
  @Column(name = "revoked", nullable = false)
  private boolean revoked = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void onCreate() {
    createdAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
