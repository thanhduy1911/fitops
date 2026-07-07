package com.fitops.identity.application.service;

public record MintedAccessToken(String accessToken, String tokenType, long expiresIn) {}
