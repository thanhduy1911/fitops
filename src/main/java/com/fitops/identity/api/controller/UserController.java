package com.fitops.identity.api.controller;

import com.fitops.identity.api.request.UpdateProfileRequest;
import com.fitops.identity.api.response.UserResponse;
import com.fitops.identity.application.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  public UserResponse me(@AuthenticationPrincipal UUID userId) {
    return userService.getProfile(userId);
  }

  @PutMapping("/me")
  public UserResponse replace(
      @AuthenticationPrincipal UUID userId, @Valid @RequestBody UpdateProfileRequest request) {
    return userService.replaceProfile(userId, request);
  }
}
