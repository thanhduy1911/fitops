package com.fitops.identity.application.service;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenService {
  String issue(UUID userId);

  Optional<UUID> rotate(String rawToken);

  void revoke(String rawToken);

  void revokeAllForUser(UUID userId);
}
