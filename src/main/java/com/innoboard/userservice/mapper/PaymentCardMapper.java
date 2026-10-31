package com.innoboard.userservice.mapper;

import com.innoboard.userservice.domain.PaymentCardEntity;
import com.innoboard.userservice.dto.CardRequest;
import com.innoboard.userservice.dto.CardResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentCardMapper {

    @Mapping(target = "userId", source = "user.id")
    CardResponse toResponse(PaymentCardEntity entity);

    List<CardResponse> toResponseList(List<PaymentCardEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    PaymentCardEntity toEntity(CardRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(CardRequest request, @MappingTarget PaymentCardEntity entity);
}
