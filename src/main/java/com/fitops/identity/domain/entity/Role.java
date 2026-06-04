package com.fitops.identity.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.*;

@Entity
@Table(schema = "identity", name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@ToString(onlyExplicitlyIncluded = true)
public class Role {
  @Id
  @ToString.Include
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ToString.Include
  @Column(name = "name", nullable = false, unique = true)
  private String name;
}
