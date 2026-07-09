package com.fitops.identity.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitops.identity.domain.entity.User;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserMapperTest {
  private final UserMapper mapper = new UserMapperImpl();

  @Test
  void mapsAllFields() {
    var createdAt = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    var user =
        User.builder()
            .email("joe.doe@fitops.com")
            .username("joe.doe123")
            .password("secret-hash")
            .displayName("Joe Doe")
            .avatarUrl("https://cdn/joe.png")
            .language("vi")
            .build();
    ReflectionTestUtils.setField(user, "createdAt", createdAt);

    var response = mapper.toResponse(user);

    assertThat(response.id()).isEqualTo(user.getId());
    assertThat(response.email()).isEqualTo("joe.doe@fitops.com");
    assertThat(response.username()).isEqualTo("joe.doe123");
    assertThat(response.displayName()).isEqualTo("Joe Doe");
    assertThat(response.avatarUrl()).isEqualTo("https://cdn/joe.png");
    assertThat(response.language()).isEqualTo("vi");
    assertThat(response.createdAt()).isEqualTo(createdAt);
  }

  @Test
  void mapsNullDisplayNameAndAvatar() {
    var user =
        User.builder()
            .email("joe.doe@fitops.com")
            .username("joe.doe123")
            .password("password-hash")
            .language("en")
            .build();
    var response = mapper.toResponse(user);
    assertThat(response.displayName()).isNull();
    assertThat(response.avatarUrl()).isNull();
  }
}
