package com.fitops.commons.config;

import com.fitops.commons.api.jackson.PatchValueModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;

@Configuration
public class JacksonConfiguration {
  @Bean
  public JacksonModule patchValueModule() {
    return new PatchValueModule();
  }
}
