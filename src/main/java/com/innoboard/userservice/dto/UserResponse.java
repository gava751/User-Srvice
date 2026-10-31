package com.innoboard.userservice.dto;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        Boolean active
) {
}
