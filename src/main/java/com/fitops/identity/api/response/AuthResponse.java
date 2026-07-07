package com.fitops.identity.api.response;

import com.fitops.identity.application.service.MintedAccessToken;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {
  public static AuthResponse from(MintedAccessToken minted) {
    return new AuthResponse(minted.accessToken(), minted.tokenType(), minted.expiresIn());
  }
}
