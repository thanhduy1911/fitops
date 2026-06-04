package com.fitops.identity.api.request;

import com.fitops.identity.api.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9_.-]+$") String username,
    @NotBlank
        @Size(min = 8, max = 72)
        @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 bytes")
        String password,
    @Size(max = 255) String displayName) {}
