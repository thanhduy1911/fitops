package com.fitops.commons.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public class PaginatedResultTest {
  @Test
  void from_firstPage_mapAllFieldCorrectly() {
    var page = new PageImpl<>(List.of("key", "value"), PageRequest.of(0, 5), 12);
    var result = PaginatedResult.from(page);

    assertThat(result.items()).containsExactly("key", "value");
    assertThat(result.totalItems()).isEqualTo(12L);
    assertThat(result.pageNumber()).isEqualTo(1);
    assertThat(result.pageSize()).isEqualTo(5);
    assertThat(result.totalPages()).isEqualTo(3);
    assertThat(result.hasPrevious()).isFalse();
    assertThat(result.hasNext()).isTrue();
  }

  @Test
  void from_lastPage_hasNextPage_hasPreviousTrue() {
    var page = new PageImpl<>(List.of("key", "value"), PageRequest.of(2, 5), 12);
    var result = PaginatedResult.from(page);

    assertThat(result.pageNumber()).isEqualTo(3);
    assertThat(result.hasPrevious()).isTrue();
    assertThat(result.hasNext()).isFalse();
  }
}
