package com.fitops.identity.application.service;

import com.fitops.identity.api.request.RegisterRequest;
import com.fitops.identity.api.response.AuthResponse;

public interface AuthService {
  AuthResponse register(RegisterRequest registerRequest);
}
