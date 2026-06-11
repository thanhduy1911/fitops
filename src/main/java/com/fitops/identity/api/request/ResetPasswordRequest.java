package com.fitops.identity.api.request;

import com.fitops.identity.api.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank String token,
    @NotBlank
        @Size(min = 8, max = 72)
        @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 bytes")
        String newPassword) {}
