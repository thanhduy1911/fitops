package com.fitops.commons.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@MappedSuperclass
@EqualsAndHashCode(of = "id")
@ToString(onlyExplicitlyIncluded = true)
public abstract class BaseEntity {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  @ToString.Include
  private UUID id;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected BaseEntity() {
    this.id = UUID.randomUUID();
  }

  @PrePersist
  protected void onCreate() {
    var now = OffsetDateTime.now(ZoneOffset.UTC);
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
