package com.fitops.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitops.identity.api.request.BodyStatRequest;
import com.fitops.identity.api.response.BodyStatResponse;
import com.fitops.identity.domain.entity.BodyStat;
import com.fitops.identity.domain.valueobject.ActivityLevel;
import com.fitops.identity.domain.valueobject.Gender;
import com.fitops.identity.infrastructure.mapper.BodyStatMapper;
import com.fitops.identity.infrastructure.persistence.BodyStatRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
public class BodyStatServiceImplTest {
  @Mock private BodyStatRepository repository;
  @Mock private BodyStatMapper mapper;

  private final Clock fixedClock =
      Clock.fixed(Instant.parse("2026-07-21T15:10:00Z"), ZoneOffset.UTC);
  private BodyStatServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new BodyStatServiceImpl(repository, mapper, fixedClock);
  }

  @Test
  void record_setsRecordedAtFromClock_andPersistsWithPrincipal() {
    var userId = UUID.randomUUID();
    var request =
        new BodyStatRequest(
            new BigDecimal("167.00"),
            new BigDecimal("75.00"),
            LocalDate.of(1999, 11, 19),
            Gender.MALE,
            ActivityLevel.VERY_ACTIVE);
    when(repository.save(any(BodyStat.class))).thenAnswer(inv -> inv.getArgument(0));
    when(mapper.toResponse(any(BodyStat.class))).thenReturn(dummyResponse());

    service.record(userId, request);

    var captor = ArgumentCaptor.forClass(BodyStat.class);
    verify(repository).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(userId);
    assertThat(saved.getRecordedAt()).isEqualTo(OffsetDateTime.now(fixedClock));
    assertThat(saved.getHeightCm()).isEqualByComparingTo("167.00");
    assertThat(saved.getGender()).isEqualTo(Gender.MALE);
  }

  @Test
  void getHistory_dropsClientSort_keepsPageAndSize() {
    var userId = UUID.randomUUID();
    var captor = ArgumentCaptor.forClass(Pageable.class);
    when(repository.findByUserIdOrderByRecordedAtDesc(eq(userId), captor.capture()))
        .thenReturn(Page.empty());

    service.getHistory(userId, PageRequest.of(2, 15, Sort.by(Sort.Direction.ASC, "weightKg")));

    var used = captor.getValue();
    assertThat(used.getSort().isUnsorted()).isTrue(); // client sort stripped
    assertThat(used.getPageNumber()).isEqualTo(2);
    assertThat(used.getPageSize()).isEqualTo(15);
  }

  private static BodyStatResponse dummyResponse() {
    return new BodyStatResponse(
        UUID.randomUUID(),
        new BigDecimal("167.00"),
        new BigDecimal("75.00"),
        LocalDate.of(1999, 11, 19),
        Gender.MALE,
        ActivityLevel.VERY_ACTIVE,
        OffsetDateTime.now());
  }
}
