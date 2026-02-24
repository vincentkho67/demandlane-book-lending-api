package com.demandlane.booklending.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.demandlane.booklending.dto.UserDto;
import com.demandlane.booklending.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper extends BaseMapper<User, UserDto.Request, UserDto.Response> {

    @Override
    UserDto.Response toResponse(User entity);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "role", expression = "java(com.demandlane.booklending.entity.Role.MEMBER)")
    User toEntity(UserDto.Request request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget User target, UserDto.Request request);
}
