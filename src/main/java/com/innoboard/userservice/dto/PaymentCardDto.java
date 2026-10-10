package com.innoboard.userservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record PaymentCardDto (
        Long id,
        Long userId,

        @NotBlank
        @Pattern(regexp = "\\d{13,19}", message = "Card number must be 13-19 digits")
        String number,

        @NotBlank
        String holder,

        @NotNull
        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate,

        @NotNull
        Boolean active
)
{ }
