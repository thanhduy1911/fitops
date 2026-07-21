package com.fitops.identity.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.identity.domain.entity.BodyStat;
import com.fitops.identity.domain.valueobject.ActivityLevel;
import com.fitops.identity.domain.valueobject.Gender;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class BodyStatMapperTest {
  private final BodyStatMapper mapper = new BodyStatMapperImpl();

  @Test
  void mapsAllFields() {
    var recordedAt = OffsetDateTime.of(2026, 7, 21, 10, 15, 30, 0, ZoneOffset.UTC);
    var bodyStat =
        BodyStat.builder()
            .userId(UUID.randomUUID())
            .heightCm(new BigDecimal("175.00"))
            .weightKg(new BigDecimal("70.00"))
            .dateOfBirth(LocalDate.of(1996, 1, 1))
            .gender(Gender.MALE)
            .activityLevel(ActivityLevel.MODERATELY_ACTIVE)
            .recordedAt(recordedAt)
            .build();

    var response = mapper.toResponse(bodyStat);

    assertThat(response.id()).isEqualTo(bodyStat.getId());
    assertThat(response.heightCm()).isEqualByComparingTo("175.00");
    assertThat(response.weightKg()).isEqualByComparingTo("70.00");
    assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1996, 1, 1));
    assertThat(response.gender()).isEqualTo(Gender.MALE);
    assertThat(response.activityLevel()).isEqualTo(ActivityLevel.MODERATELY_ACTIVE);
    assertThat(response.recordedAt()).isEqualTo(recordedAt);
  }
}
