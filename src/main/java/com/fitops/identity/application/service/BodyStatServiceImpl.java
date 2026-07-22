package com.fitops.identity.application.service;

import com.fitops.commons.api.PaginatedResult;
import com.fitops.identity.api.request.BodyStatRequest;
import com.fitops.identity.api.response.BodyStatResponse;
import com.fitops.identity.domain.entity.BodyStat;
import com.fitops.identity.infrastructure.mapper.BodyStatMapper;
import com.fitops.identity.infrastructure.persistence.BodyStatRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BodyStatServiceImpl implements BodyStatService {
  private final BodyStatRepository bodyStatRepository;
  private final BodyStatMapper bodyStatMapper;
  private final Clock clock;

  public BodyStatServiceImpl(
      BodyStatRepository bodyStatRepository, BodyStatMapper bodyStatMapper, Clock clock) {
    this.bodyStatRepository = bodyStatRepository;
    this.bodyStatMapper = bodyStatMapper;
    this.clock = clock;
  }

  @Transactional
  @Override
  public BodyStatResponse record(UUID userId, BodyStatRequest request) {
    var bodyStat =
        BodyStat.builder()
            .userId(userId)
            .heightCm(request.heightCm())
            .weightKg(request.weightKg())
            .dateOfBirth(request.dateOfBirth())
            .activityLevel(request.activityLevel())
            .gender(request.gender())
            .recordedAt(OffsetDateTime.now(clock))
            .build();

    return bodyStatMapper.toResponse(bodyStatRepository.save(bodyStat));
  }

  @Override
  public PaginatedResult<BodyStatResponse> getHistory(UUID userId, Pageable pageable) {
    // Ordering is fixed to recordedAt DESC (method name); drop any client-supplied
    // sort so history order is not client-controllable.
    var pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    return PaginatedResult.from(
        bodyStatRepository
            .findByUserIdOrderByRecordedAtDesc(userId, pageRequest)
            .map(bodyStatMapper::toResponse));
  }
}
