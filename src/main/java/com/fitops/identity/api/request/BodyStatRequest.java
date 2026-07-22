package com.fitops.identity.api.request;

import com.fitops.identity.api.validation.ValidAge;
import com.fitops.identity.domain.valueobject.ActivityLevel;
import com.fitops.identity.domain.valueobject.Gender;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BodyStatRequest(
    @NotNull @DecimalMin("50.0") @DecimalMax("300.0") @Digits(integer = 3, fraction = 2)
        BigDecimal heightCm,
    @NotNull @Positive @DecimalMax("500.0") @Digits(integer = 3, fraction = 2) BigDecimal weightKg,
    @NotNull @ValidAge(min = 13, max = 120) LocalDate dateOfBirth,
    @NotNull Gender gender,
    @NotNull ActivityLevel activityLevel) {}
