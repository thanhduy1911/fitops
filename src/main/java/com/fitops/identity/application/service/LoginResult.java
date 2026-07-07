package com.fitops.identity.application.service;

public record LoginResult(MintedAccessToken accessToken, String rawRefreshToken) {}
