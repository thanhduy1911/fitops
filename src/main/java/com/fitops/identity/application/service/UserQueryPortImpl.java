package com.fitops.identity.application.service;

import com.fitops.identity.application.port.UserQueryPort;
import com.fitops.identity.application.port.UserSummary;
import com.fitops.identity.infrastructure.persistence.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserQueryPortImpl implements UserQueryPort {
  private final UserRepository userRepository;

  public UserQueryPortImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public Optional<UserSummary> findById(UUID userId) {
    return userRepository.findActiveSummaryById(userId);
  }

  @Override
  public boolean existsById(UUID userId) {
    return userRepository.existsActiveById(userId);
  }
}
