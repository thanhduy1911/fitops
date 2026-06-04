package com.fitops.identity.infrastructure.persistence;

import com.fitops.identity.domain.entity.User;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
  boolean existsByEmail(String email);

  boolean existsByUsernameIgnoreCase(String username);

  @Query("SELECT user FROM User user LEFT JOIN FETCH user.roles WHERE user.email = :email")
  Optional<User> findByEmail(@Param("email") String email);
}
