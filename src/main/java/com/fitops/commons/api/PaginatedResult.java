package com.fitops.commons.api;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginatedResult<T>(
    List<T> items,
    long totalItems,
    int pageNumber,
    int pageSize,
    int totalPages,
    boolean hasPrevious,
    boolean hasNext) {

  public static <T> PaginatedResult<T> from(Page<T> page) {
    return new PaginatedResult<>(
        page.getContent(),
        page.getTotalElements(),
        page.getNumber() + 1,
        page.getSize(),
        page.getTotalPages(),
        page.hasPrevious(),
        page.hasNext());
  }
}
