package com.fitops.identity.api.request;

import com.fitops.commons.api.PatchValue;
import com.fitops.commons.api.validation.NotExplicitlyNull;
import com.fitops.identity.api.validation.SupportedLanguage;
import jakarta.validation.constraints.Size;
import jakarta.validation.valueextraction.Unwrapping;
import org.hibernate.validator.constraints.URL;

public record PatchProfileRequest(
    @Size(max = 255) PatchValue<String> displayName,
    // Skip = validate the container, not the unwrapped value: reject an explicit null,
    // but allow the field to be omitted. Without Skip, @UnwrapByDefault hands this the
    // inner String and it can no longer tell absent from explicitly-null. Do not delete.
    @NotExplicitlyNull(payload = Unwrapping.Skip.class) @SupportedLanguage
        PatchValue<String> language,
    @Size(max = 500) @URL(protocol = "https") PatchValue<String> avatarUrl) {

  public PatchProfileRequest {
    // Mandatory: Jackson never produces Java-null here, but direct construction (tests) can.
    // A Java-null field is never handed to the ValueExtractor, so its constraints would be
    // silently skipped rather than failing loudly. Normalise to undefined.
    displayName = displayName == null ? PatchValue.undefined() : displayName;
    language = language == null ? PatchValue.undefined() : language;
    avatarUrl = avatarUrl == null ? PatchValue.undefined() : avatarUrl;
  }
}
