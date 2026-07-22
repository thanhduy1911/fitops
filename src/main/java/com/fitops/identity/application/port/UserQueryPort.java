package com.fitops.identity.application.port;

import java.util.Optional;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/**
 * The only sanctioned way another module reads a user from {@code identity}.
 *
 * <p>Consumers depend on this interface and never on {@code UserRepository} or any other identity
 * internal; {@code ApplicationModules.verify()} fails the build if they do.
 *
 * <p><strong>Active users only.</strong> A deactivated user ({@code is_active = false}) is reported
 * exactly as a non-existent one, {@code Optional.empty()} / {@code false}. This matches how the
 * rest of identity reads users. No inactive-visible variant exists yet; it will be added by the
 * first consumer that genuinely needs one.
 */
@NamedInterface("port")
public interface UserQueryPort {
  /**
   * @return the summary of the active user with this id, or empty if unknown or deactivated.
   */
  Optional<UserSummary> findById(UUID userId);

  /**
   * @return whether an active user with this id exists.
   */
  boolean existsById(UUID userId);
}
