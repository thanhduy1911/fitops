package com.fitops.identity.infrastructure.mapper;

import com.fitops.identity.api.response.BodyStatResponse;
import com.fitops.identity.domain.entity.BodyStat;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BodyStatMapper {
  BodyStatResponse toResponse(BodyStat bodyStat);
}
