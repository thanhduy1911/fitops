package com.fitops.identity.application.service;

import com.fitops.commons.constants.ErrorCode;
import com.fitops.commons.exception.ConflictException;
import com.fitops.commons.exception.UnauthorizedException;
import com.fitops.commons.security.JwtProperties;
import com.fitops.commons.security.JwtService;
import com.fitops.identity.api.request.LoginRequest;
import com.fitops.identity.api.request.RegisterRequest;
import com.fitops.identity.api.response.AuthResponse;
import com.fitops.identity.domain.entity.Role;
import com.fitops.identity.domain.entity.User;
import com.fitops.identity.domain.event.UserRegisteredEvent;
import com.fitops.identity.infrastructure.persistence.RoleRepository;
import com.fitops.identity.infrastructure.persistence.UserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
  private static final String ROLE_USER = "ROLE_USER";
  private static final String DUMMY_RAW_TOKEN = "fitops-timing-decoy-not-a-real-password";
  private final String dummyHash;

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtProperties jwtProperties;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;

  public AuthServiceImpl(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      JwtProperties jwtProperties,
      ApplicationEventPublisher applicationEventPublisher,
      JwtService jwtService,
      RefreshTokenService refreshTokenService) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtProperties = jwtProperties;
    this.applicationEventPublisher = applicationEventPublisher;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.dummyHash = passwordEncoder.encode(DUMMY_RAW_TOKEN);
  }

  @Override
  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    String username = request.username().trim();

    if (userRepository.existsByEmail(email)) {
      throw new ConflictException(ErrorCode.AUTH_004, "Email already registered");
    }
    if (userRepository.existsByUsernameIgnoreCase(username)) {
      throw new ConflictException(ErrorCode.AUTH_005, "Username already taken");
    }

    var roleUser =
        roleRepository
            .findByName(ROLE_USER)
            .orElseThrow(() -> new IllegalStateException("Seed role ROLE_USER is missing"));
    var user =
        User.builder()
            .email(email)
            .username(username)
            .password(passwordEncoder.encode(request.password()))
            .displayName(request.displayName())
            .build();
    user.getRoles().add(roleUser);

    try {
      userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException exception) {
      throw mapUniqueViolation(exception);
    }

    applicationEventPublisher.publishEvent(new UserRegisteredEvent(user.getId(), Instant.now()));
    var accessToken = jwtService.generate(user.getId(), Set.of(ROLE_USER));
    var expiresIn = jwtProperties.accessTokenTtl().toSeconds();
    // TODO:instant 7-day session on signup - see FO-0045 "Deferred" section
    return new AuthResponse(accessToken, "Bearer", expiresIn);
  }

  @Override
  public LoginResult login(LoginRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    var userOpt = userRepository.findByEmail(email);
    if (userOpt.isEmpty()) {
      passwordEncoder.matches(request.password(), dummyHash);
      throw new UnauthorizedException(ErrorCode.AUTH_007, "Invalid credentials");
    }
    var user = userOpt.get();
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new UnauthorizedException(ErrorCode.AUTH_007, "Invalid credentials");
    }
    if (!user.isActive()) {
      // TODO: friendlier "account disabled" (AUTH_008) — see deactivation ticket
      //   <https://www.notion.so/37528828fc3a81f7a828d85c8d5b9fad>
      throw new UnauthorizedException(ErrorCode.AUTH_007, "Invalid credentials");
    }
    var roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    var accessToken = jwtService.generate(user.getId(), roles);
    var expiresIn = jwtProperties.accessTokenTtl().toSeconds();
    var rawRefreshToken = refreshTokenService.issue(user.getId());

    return new LoginResult(accessToken, expiresIn, rawRefreshToken);
  }

  private RuntimeException mapUniqueViolation(DataIntegrityViolationException exception) {
    String constraint = "";
    if (exception.getCause() instanceof org.hibernate.exception.ConstraintViolationException cve
        && cve.getConstraintName() != null) {
      constraint = cve.getConstraintName().toLowerCase(Locale.ROOT);
    }
    if (constraint.contains("email")) {
      throw new ConflictException(ErrorCode.AUTH_004, "Email already registered");
    }
    if (constraint.contains("username")) {
      throw new ConflictException(ErrorCode.AUTH_005, "Username already taken");
    }
    return exception;
  }
}
