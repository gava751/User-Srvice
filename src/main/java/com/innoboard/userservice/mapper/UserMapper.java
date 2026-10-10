package com.innoboard.userservice.mapper;


import com.innoboard.userservice.domain.UserEntity;
import com.innoboard.userservice.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

    UserDto toDto(UserEntity entity);

    List<UserDto> toDtoList(List<UserEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentCards",ignore = true)
    UserEntity toEntity(UserDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentCards", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(UserDto dto,@MappingTarget UserEntity entity);
}
