package com.fitops.identity.api.request;

import com.fitops.identity.api.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Email @Size(max = 255) String email, @NotBlank @MaxUtf8Bytes(72) String password) {}
