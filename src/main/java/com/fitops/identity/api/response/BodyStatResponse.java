package com.fitops.identity.api.response;

import com.fitops.identity.domain.valueobject.ActivityLevel;
import com.fitops.identity.domain.valueobject.Gender;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BodyStatResponse(
    UUID id,
    BigDecimal heightCm,
    BigDecimal weightKg,
    LocalDate dateOfBirth,
    Gender gender,
    ActivityLevel activityLevel,
    OffsetDateTime recordedAt) {}
