package com.fitops.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fitops.commons.constants.ErrorCode;
import com.fitops.commons.exception.ConflictException;
import com.fitops.commons.exception.UnauthorizedException;
import com.fitops.identity.api.request.LoginRequest;
import com.fitops.identity.api.request.RegisterRequest;
import com.fitops.identity.application.port.PasswordResetMailer;
import com.fitops.identity.domain.entity.Role;
import com.fitops.identity.domain.entity.User;
import com.fitops.identity.domain.event.UserRegisteredEvent;
import com.fitops.identity.infrastructure.persistence.RoleRepository;
import com.fitops.identity.infrastructure.persistence.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private PasswordResetService passwordResetService;
  @Mock private PasswordResetMailer passwordResetMailer;
  @Mock private AccessTokenMinter accessTokenMinter;

  private AuthServiceImpl authServiceImpl;
  private RegisterRequest registerRequest;
  private LoginRequest loginRequest;

  @BeforeEach
  void setUp() {
    authServiceImpl =
        new AuthServiceImpl(
            userRepository,
            roleRepository,
            passwordEncoder,
            applicationEventPublisher,
            accessTokenMinter,
            refreshTokenService,
            passwordResetService,
            passwordResetMailer);
    registerRequest =
        new RegisterRequest("Joe.Doe@FitOps.com", "John.Doe123", "password123", "John Doe");
    loginRequest = new LoginRequest("Joe.Doe@FitOps.com", "password123");
  }

  @Test
  @DisplayName("Should register user, persist normalized data, publish event, return token")
  void register_Success() {
    var roleUser = mock(Role.class);
    when(userRepository.existsByEmail("joe.doe@fitops.com")).thenReturn(false);
    when(userRepository.existsByUsernameIgnoreCase("John.Doe123")).thenReturn(false);
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleUser));
    when(passwordEncoder.encode("password123")).thenReturn("hashedPw");
    when(accessTokenMinter.mint(any(), any()))
        .thenReturn(new MintedAccessToken("jwt-token", "Bearer", 3600L));

    var response = authServiceImpl.register(registerRequest);

    assertThat(response.accessToken()).isEqualTo("jwt-token");
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.expiresIn()).isEqualTo(3600L);
    verify(accessTokenMinter).mint(any(), eq(Set.of("ROLE_USER")));

    var userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).saveAndFlush(userCaptor.capture());
    var saved = userCaptor.getValue();
    assertThat(saved.getEmail()).isEqualTo("joe.doe@fitops.com");
    assertThat(saved.getUsername()).isEqualTo("John.Doe123");
    assertThat(saved.getPassword()).isEqualTo("hashedPw");
    assertThat(saved.getDisplayName()).isEqualTo("John Doe");
    assertThat(saved.getRoles()).contains(roleUser);

    var eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().userId()).isEqualTo(saved.getId());
  }

  @Test
  @DisplayName("Duplicate email (pre-check) should throw 409 (AUTH_004)")
  void register_DuplicateEmail_throwsAuth004() {
    when(userRepository.existsByEmail("joe.doe@fitops.com")).thenReturn(true);
    assertThatThrownBy(() -> authServiceImpl.register(registerRequest))
        .isInstanceOf(ConflictException.class)
        .extracting(exception -> ((ConflictException) exception).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_004);
    verify(passwordEncoder, never()).encode("password123");
    verify(userRepository, never()).saveAndFlush(any());
    verifyNoInteractions(applicationEventPublisher, accessTokenMinter);
  }

  @Test
  @DisplayName("Duplicate username (pre-check) should throw 409 (AUTH_005)")
  void register_DuplicateUsername_throwsAuth005() {
    when(userRepository.existsByEmail("joe.doe@fitops.com")).thenReturn(false);
    when(userRepository.existsByUsernameIgnoreCase("John.Doe123")).thenReturn(true);
    assertThatThrownBy(() -> authServiceImpl.register(registerRequest))
        .isInstanceOf(ConflictException.class)
        .extracting(exception -> ((ConflictException) exception).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_005);
    verify(passwordEncoder, never()).encode("password123");
    verify(userRepository, never()).saveAndFlush(any());
    verifyNoInteractions(applicationEventPublisher, accessTokenMinter);
  }

  @Test
  @DisplayName(
      "Race: unique violation on flush mapped by constraint name should throw 409 (AUTH_005)")
  void register_UniqueViolationOnFlush_mapsToAuth005() {
    var roleUser = mock(Role.class);
    when(userRepository.existsByEmail("joe.doe@fitops.com")).thenReturn(false);
    when(userRepository.existsByUsernameIgnoreCase("John.Doe123")).thenReturn(false);
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleUser));
    when(passwordEncoder.encode("password123")).thenReturn("hashedPw");

    var cve = mock(ConstraintViolationException.class);
    when(cve.getConstraintName()).thenReturn("uq_users_username_lower");
    when(userRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("duplicated", cve));

    assertThatThrownBy(() -> authServiceImpl.register(registerRequest))
        .isInstanceOf(ConflictException.class)
        .extracting(exception -> ((ConflictException) exception).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_005);

    verify(applicationEventPublisher, never()).publishEvent(any());
    verifyNoInteractions(accessTokenMinter);
  }

  @Test
  @DisplayName("Unknown constraint should rethrow original error")
  void register_UnknownConstraint_rethrowsOriginal() {
    var roleUser = mock(Role.class);
    when(userRepository.existsByEmail("joe.doe@fitops.com")).thenReturn(false);
    when(userRepository.existsByUsernameIgnoreCase("John.Doe123")).thenReturn(false);
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleUser));
    when(passwordEncoder.encode("password123")).thenReturn("hashedPw");
    var cve = mock(ConstraintViolationException.class);
    when(cve.getConstraintName()).thenReturn("some_unrelate_constraint");
    when(userRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("Internal Error", cve));
    assertThatThrownBy(() -> authServiceImpl.register(registerRequest))
        .isInstanceOf(DataIntegrityViolationException.class);
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Missing ROLE_USER seed should throw IllegalStateException")
  void register_MissingRoleUserSeed_throwsIllegalStateException() {
    when(userRepository.existsByEmail("joe.doe@fitops.com")).thenReturn(false);
    when(userRepository.existsByUsernameIgnoreCase("John.Doe123")).thenReturn(false);
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authServiceImpl.register(registerRequest))
        .isInstanceOf(IllegalStateException.class);

    verify(passwordEncoder, never()).encode("password123");
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("Login success then token carries user's REAL roles, returns LoginResult")
  void login_success() {
    var role = mock(Role.class);
    when(role.getName()).thenReturn("ROLE_USER");
    var user = User.builder().email("joe.doe@fitops.com").password("hashedPw").build();
    user.getRoles().add(role);

    when(userRepository.findByEmail("joe.doe@fitops.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "hashedPw")).thenReturn(true);
    when(accessTokenMinter.mint(any(), any()))
        .thenReturn(new MintedAccessToken("jwt", "Bearer", 3600L));
    when(refreshTokenService.issue(user.getId())).thenReturn("raw");

    var result = authServiceImpl.login(loginRequest);

    assertThat(result.accessToken().accessToken()).isEqualTo("jwt");
    assertThat(result.accessToken().expiresIn()).isEqualTo(3600L);
    assertThat(result.rawRefreshToken()).isEqualTo("raw");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Set<String>> rolesCaptor = ArgumentCaptor.forClass(Set.class);
    verify(accessTokenMinter).mint(eq(user.getId()), rolesCaptor.capture());
    assertThat(rolesCaptor.getValue()).containsExactly("ROLE_USER");
  }

  @Test
  @DisplayName("Wrong password throws AUTH_007. No refresh issued, no token minted")
  void login_wrongPassword_throwsAuth007() {
    var user = User.builder().email("joe.doe@fitops.com").password("hashedPw").build();
    when(userRepository.findByEmail("joe.doe@fitops.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "hashedPw")).thenReturn(false);

    assertThatThrownBy(() -> authServiceImpl.login(loginRequest))
        .isInstanceOf(UnauthorizedException.class)
        .extracting(e -> ((UnauthorizedException) e).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_007);

    verify(refreshTokenService, never()).issue(any());
    verifyNoInteractions(accessTokenMinter);
  }

  @Test
  @DisplayName("Unknown email throws AUTH_007. Decoy BCrypt ran (constant-time), no refresh issued")
  void login_unknownEmail_throwsAuth007_afterDecoy() {
    when(userRepository.findByEmail("joe.doe@fitops.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authServiceImpl.login(loginRequest))
        .isInstanceOf(UnauthorizedException.class)
        .extracting(e -> ((UnauthorizedException) e).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_007);

    verify(passwordEncoder).matches(eq("password123"), any());
    verify(refreshTokenService, never()).issue(any());
    verifyNoInteractions(accessTokenMinter);
  }

  @Test
  @DisplayName(
      "Deactivated user (correct password) throws AUTH_007, Indistinguishable from wrong pw")
  void login_deactivated_throwsAuth007() {
    var user = User.builder().email("joe.doe@fitops.com").password("hashedPw").build();
    user.setActive(false);
    when(userRepository.findByEmail("joe.doe@fitops.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "hashedPw")).thenReturn(true);

    assertThatThrownBy(() -> authServiceImpl.login(loginRequest))
        .isInstanceOf(UnauthorizedException.class)
        .extracting(e -> ((UnauthorizedException) e).getErrorCode())
        .isEqualTo(ErrorCode.AUTH_007);

    verify(refreshTokenService, never()).issue(any());
    verifyNoInteractions(accessTokenMinter);
  }
}
