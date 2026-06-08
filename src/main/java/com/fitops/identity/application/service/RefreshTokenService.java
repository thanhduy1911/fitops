package com.fitops.identity.application.service;

import java.util.UUID;

public interface RefreshTokenService {
  String issue(UUID userId);
}
