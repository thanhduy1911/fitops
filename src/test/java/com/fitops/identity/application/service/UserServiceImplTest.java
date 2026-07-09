package com.fitops.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fitops.commons.constants.ErrorCode;
import com.fitops.commons.exception.UnauthorizedException;
import com.fitops.identity.api.request.UpdateProfileRequest;
import com.fitops.identity.api.response.UserResponse;
import com.fitops.identity.domain.entity.User;
import com.fitops.identity.infrastructure.mapper.UserMapper;
import com.fitops.identity.infrastructure.persistence.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
  @Mock private UserRepository userRepository;
  @Mock private UserMapper userMapper;

  private UserServiceImpl userService;

  @BeforeEach
  void setUp() {
    userService = new UserServiceImpl(userRepository, userMapper);
  }

  @Test
  void getProfile_activeUser_returnsMapped() {
    var user = activeUser();
    var expected = dummyResponse();
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(userMapper.toResponse(user)).thenReturn(expected);

    assertThat(userService.getProfile(user.getId())).isSameAs(expected);
  }

  @Test
  void getProfile_missingUser_throwsAuth001() {
    var id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getProfile(id))
        .isInstanceOf(UnauthorizedException.class)
        .extracting(e -> ((UnauthorizedException) e).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_001);
    verifyNoInteractions(userMapper);
  }

  @Test
  void getProfile_inactiveUser_throwsAuth001() {
    var user = activeUser();
    user.setActive(false);
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    assertThatThrownBy(() -> userService.getProfile(user.getId()))
        .isInstanceOf(UnauthorizedException.class)
        .extracting(e -> ((UnauthorizedException) e).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_001);
  }

  @Test
  void replaceProfile_normalizesAndClears() {
    var user = activeUser();
    user.setDisplayName("old");
    user.setAvatarUrl("https://old/a.png");
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(userMapper.toResponse(user)).thenReturn(dummyResponse());

    userService.replaceProfile(user.getId(), new UpdateProfileRequest("   ", "EN", null));

    assertThat(user.getDisplayName()).isNull(); // blank -> null
    assertThat(user.getLanguage()).isEqualTo("en"); // lower-cased
    assertThat(user.getAvatarUrl()).isNull(); // omitted -> cleared
  }

  @Test
  void replaceProfile_trimsDisplayName_setsAvatar() {
    var user = activeUser();
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(userMapper.toResponse(user)).thenReturn(dummyResponse());

    userService.replaceProfile(
        user.getId(), new UpdateProfileRequest("  Bob  ", "vi", "https://cdn/bob.png"));

    assertThat(user.getDisplayName()).isEqualTo("Bob");
    assertThat(user.getAvatarUrl()).isEqualTo("https://cdn/bob.png");
  }

  private static User activeUser() {
    return User.builder()
        .email("joe@fitops.com")
        .username("joe")
        .password("hash")
        .language("vi")
        .build();
  }

  private static UserResponse dummyResponse() {
    return new UserResponse(
        UUID.randomUUID(), "joe@fitops.com", "joe", null, null, "vi", OffsetDateTime.now());
  }
}
