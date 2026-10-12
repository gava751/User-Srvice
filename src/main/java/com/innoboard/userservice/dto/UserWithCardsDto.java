package com.innoboard.userservice.dto;

import java.util.List;

public record UserWithCardsDto (
        UserDto user,
        List<PaymentCardDto> cards
){}
