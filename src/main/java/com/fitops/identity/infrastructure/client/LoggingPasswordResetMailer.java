package com.fitops.identity.infrastructure.client;

import com.fitops.commons.security.PasswordResetProperties;
import com.fitops.identity.application.port.PasswordResetMailer;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class LoggingPasswordResetMailer implements PasswordResetMailer {
  private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetMailer.class);
  private final PasswordResetProperties properties;

  public LoggingPasswordResetMailer(PasswordResetProperties properties) {
    this.properties = properties;
  }

  // TODO: Remember to comeback to delete the raw token out of the log
  @Override
  public void sendResetLink(String email, String rawToken, OffsetDateTime expiresAt) {
    log.info(
        "[DEV] Password reset link for {} (expires {}): {}?token={}",
        email,
        expiresAt,
        properties.resetUrlBase(),
        rawToken);
  }
}
