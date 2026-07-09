package com.fitops.identity.api.request;

import com.fitops.identity.api.validation.SupportedLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateProfileRequest(
    @Size(max = 255) String displayName,
    @NotBlank @SupportedLanguage String language,
    @Size(max = 500) @URL(protocol = "https") String avatarUrl) {}
