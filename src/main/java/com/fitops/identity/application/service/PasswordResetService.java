package com.fitops.identity.application.service;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetService {
  /** Invalidate the user's prior active tokens, then mint one. */
  IssuedResetToken issue(UUID userId);

  /** Validate (exists, not consumed, not expired) and consume. Returns userId on success. */
  Optional<UUID> consume(String rawToken);
}
