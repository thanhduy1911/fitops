package com.fitops.identity.api.controller;

import com.fitops.commons.api.PaginatedResult;
import com.fitops.identity.api.request.BodyStatRequest;
import com.fitops.identity.api.response.BodyStatResponse;
import com.fitops.identity.application.service.BodyStatService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/body-stats")
public class BodyStatController {
  private final BodyStatService bodyStatService;

  public BodyStatController(BodyStatService bodyStatService) {
    this.bodyStatService = bodyStatService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BodyStatResponse record(
      @AuthenticationPrincipal UUID userId, @Valid @RequestBody BodyStatRequest request) {
    return bodyStatService.record(userId, request);
  }

  @GetMapping
  public PaginatedResult<BodyStatResponse> history(
      @AuthenticationPrincipal UUID userId, Pageable pageable) {
    return bodyStatService.getHistory(userId, pageable);
  }
}
