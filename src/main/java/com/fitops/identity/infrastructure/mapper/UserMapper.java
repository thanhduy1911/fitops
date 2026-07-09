package com.fitops.identity.infrastructure.mapper;

import com.fitops.identity.api.response.UserResponse;
import com.fitops.identity.domain.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
  UserResponse toResponse(User user);
}
