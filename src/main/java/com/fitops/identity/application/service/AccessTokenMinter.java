package com.fitops.identity.application.service;

import java.util.Set;
import java.util.UUID;

public interface AccessTokenMinter {
  MintedAccessToken mint(UUID userId, Set<String> roles);
}
