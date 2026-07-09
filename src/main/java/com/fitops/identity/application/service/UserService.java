package com.fitops.identity.application.service;

import com.fitops.identity.api.request.UpdateProfileRequest;
import com.fitops.identity.api.response.UserResponse;
import java.util.UUID;

public interface UserService {
    UserResponse getProfile(UUID userId);
    UserResponse replaceProfile(UUID userId, UpdateProfileRequest request);
}
