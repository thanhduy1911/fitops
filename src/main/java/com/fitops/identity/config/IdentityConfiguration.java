package com.fitops.identity.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RefreshTokenProperties.class, PasswordResetProperties.class})
public class IdentityConfiguration {}
