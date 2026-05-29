package com.fitops.commons.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ServiceHeader {
  REQUEST_ID_HEADER("X-Request-Id"),
  ;
  private final String headerName;
}
