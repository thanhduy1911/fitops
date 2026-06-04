package com.fitops.identity.domain.entity;

import com.fitops.commons.domain.entity.BaseEntity;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(schema = "identity", name = "users")
@DynamicInsert // lets unset nullable columns fall back to DB defaults
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class User extends BaseEntity {

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @ToString.Include
  @Column(name = "username", nullable = false, unique = true)
  private String username;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Builder.Default
  @Column(name = "language", nullable = false)
  private String language = "vi";

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  @Builder.Default
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      schema = "identity",
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();
}
