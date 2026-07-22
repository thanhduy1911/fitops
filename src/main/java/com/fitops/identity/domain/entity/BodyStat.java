package com.fitops.identity.domain.entity;

import com.fitops.identity.domain.valueobject.ActivityLevel;
import com.fitops.identity.domain.valueobject.Gender;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(schema = "identity", name = "body_stats")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(onlyExplicitlyIncluded = true)
public class BodyStat {

  @Id
  @ToString.Include
  @Builder.Default
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id = UUID.randomUUID();

  @Column(name = "user_id", updatable = false, nullable = false)
  private UUID userId;

  @Column(name = "height_cm", updatable = false, nullable = false, precision = 5, scale = 2)
  private BigDecimal heightCm;

  @Column(name = "weight_kg", updatable = false, nullable = false, precision = 5, scale = 2)
  private BigDecimal weightKg;

  @Column(name = "date_of_birth", updatable = false, nullable = false)
  private LocalDate dateOfBirth;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender", updatable = false, nullable = false)
  private Gender gender;

  @Enumerated(EnumType.STRING)
  @Column(name = "activity_level", updatable = false, nullable = false)
  private ActivityLevel activityLevel;

  @Column(name = "recorded_at", updatable = false, nullable = false)
  private OffsetDateTime recordedAt;
}
