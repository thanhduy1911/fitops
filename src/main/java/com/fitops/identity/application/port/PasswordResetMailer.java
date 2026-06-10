package com.fitops.identity.application.port;

import java.time.OffsetDateTime;

public interface PasswordResetMailer {
  void sendResetLink(String email, String rawToken, OffsetDateTime expiresAt);
}
