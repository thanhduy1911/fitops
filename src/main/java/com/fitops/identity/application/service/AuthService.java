package com.fitops.identity.application.service;

import com.fitops.identity.api.request.ForgotPasswordRequest;
import com.fitops.identity.api.request.LoginRequest;
import com.fitops.identity.api.request.RegisterRequest;
import com.fitops.identity.api.request.ResetPasswordRequest;

public interface AuthService {
  MintedAccessToken register(RegisterRequest registerRequest);

  LoginResult login(LoginRequest request);

  LoginResult refresh(String rawRefreshToken);

  void logout(String rawRefreshToken);

  void forgotPassword(ForgotPasswordRequest request);

  void resetPassword(ResetPasswordRequest request);
}
