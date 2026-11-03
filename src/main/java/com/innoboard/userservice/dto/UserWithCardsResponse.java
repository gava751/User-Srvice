package com.innoboard.userservice.dto;

import java.util.List;

public record UserWithCardsResponse(
        UserResponse user,
        List<CardResponse> cards
) {
}
