package com.fitops.identity.application.service;

public record LoginResult(String accessToken, long expiresIn, String rawRefreshToken) {}
