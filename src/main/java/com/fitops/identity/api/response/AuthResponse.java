package com.fitops.identity.api.response;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {}
