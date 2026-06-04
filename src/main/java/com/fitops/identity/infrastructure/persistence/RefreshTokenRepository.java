package com.fitops.identity.infrastructure.persistence;

import com.fitops.identity.domain.entity.RefreshToken;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {}
