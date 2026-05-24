package com.fitops.commons.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MDCConstant {
  REQUEST_ID("request_id");
  private final String key;
}
