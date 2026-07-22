package com.fitops.identity.application.service;

import com.fitops.commons.api.PaginatedResult;
import com.fitops.identity.api.request.BodyStatRequest;
import com.fitops.identity.api.response.BodyStatResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface BodyStatService {
  BodyStatResponse record(UUID userId, BodyStatRequest bodyStatRequest);

  PaginatedResult<BodyStatResponse> getHistory(UUID userId, Pageable pageable);
}
