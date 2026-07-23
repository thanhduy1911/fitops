package com.fitops.identity.application.service;

import com.fitops.commons.constants.ErrorCode;
import com.fitops.commons.exception.UnauthorizedException;
import com.fitops.identity.api.request.PatchProfileRequest;
import com.fitops.identity.api.request.UpdateProfileRequest;
import com.fitops.identity.api.response.UserResponse;
import com.fitops.identity.domain.entity.User;
import com.fitops.identity.infrastructure.mapper.UserMapper;
import com.fitops.identity.infrastructure.persistence.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
    this.userRepository = userRepository;
    this.userMapper = userMapper;
  }

  @Override
  public UserResponse getProfile(UUID userId) {
    return userMapper.toResponse(loadActiveUser(userId));
  }

  @Override
  @Transactional
  public UserResponse replaceProfile(UUID userId, UpdateProfileRequest request) {
    var user = loadActiveUser(userId);
    user.setDisplayName(normalizeDisplayName(request.displayName()));
    user.setLanguage(request.language().toLowerCase(Locale.ROOT));
    user.setAvatarUrl(request.avatarUrl());
    return userMapper.toResponse(user);
  }

  @Override
  @Transactional
  public UserResponse patchProfile(UUID userId, PatchProfileRequest request) {
    var user = loadActiveUser(userId);
    request
        .displayName()
        .ifPresent(displayName -> user.setDisplayName(normalizeDisplayName(displayName)));
    request.language().ifPresent(language -> user.setLanguage(language.toLowerCase(Locale.ROOT)));
    request.avatarUrl().ifPresent(user::setAvatarUrl);
    return userMapper.toResponse(user);
  }

  private static String normalizeDisplayName(String displayName) {
    if (StringUtils.isBlank(displayName)) {
      return null;
    }
    return displayName.trim();
  }

  private User loadActiveUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(User::isActive)
        .orElseThrow(
            () -> new UnauthorizedException(ErrorCode.AUTH_001, "Authentication required"));
  }
}
