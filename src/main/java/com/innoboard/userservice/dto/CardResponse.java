package com.innoboard.userservice.dto;

import java.time.LocalDate;

public record CardResponse(
        Long id,
        Long userId,
        String number,
        String holder,
        LocalDate expirationDate,
        Boolean active
) {
}
