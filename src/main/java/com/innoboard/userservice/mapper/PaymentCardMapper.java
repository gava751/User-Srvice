package com.innoboard.userservice.mapper;

import com.innoboard.userservice.domain.PaymentCardEntity;
import com.innoboard.userservice.dto.PaymentCardDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentCardMapper {

    List<PaymentCardDto> toDtoList(List<PaymentCardEntity> entities);

    @Mapping(target = "userId", source = "user.id")
    PaymentCardDto toDto(PaymentCardEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    PaymentCardEntity toEntity(PaymentCardDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntityFromDto(PaymentCardDto dto, @MappingTarget PaymentCardEntity entity);
}
