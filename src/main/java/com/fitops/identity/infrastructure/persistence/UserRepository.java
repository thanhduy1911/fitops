package com.fitops.identity.infrastructure.persistence;

import com.fitops.identity.application.port.UserSummary;
import com.fitops.identity.domain.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
  boolean existsByEmail(String email);

  boolean existsByUsernameIgnoreCase(String username);

  @Query(
      "SELECT user "
          + "FROM User user "
          + "LEFT JOIN FETCH user.roles "
          + "WHERE user.email = :email")
  Optional<User> findByEmail(@Param("email") String email);

  @Query(
      "SELECT "
          + "user "
          + "FROM User user "
          + "LEFT JOIN FETCH user.roles "
          + "WHERE user.id = :id")
  Optional<User> findByIdWithRoles(@Param("id") UUID id);

  @Query(
      """
      SELECT new com.fitops.identity.application.port.UserSummary(
             user.id, user.username, user.displayName, user.language)
      FROM User user
      WHERE user.id = :id AND user.isActive = true
      """)
  Optional<UserSummary> findActiveSummaryById(@Param("id") UUID id);

  @Query(
      """
      SELECT COUNT(user) > 0 FROM User user
      WHERE user.id = :id AND user.isActive = true
      """)
  boolean existsActiveById(@Param("id") UUID id);
}
