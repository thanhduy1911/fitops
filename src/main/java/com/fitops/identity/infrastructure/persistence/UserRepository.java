package com.fitops.identity.infrastructure.persistence;

import com.fitops.identity.domain.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  boolean existsByEmail(String email);

  boolean existsByUsernameIgnoreCase(String username);
}
