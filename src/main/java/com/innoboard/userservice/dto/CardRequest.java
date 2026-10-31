package com.innoboard.userservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record CardRequest(

        @NotBlank(message = "Card number must not be blank")
        @Pattern(regexp = "\\d{13,19}", message = "Card number must be 13-19 digits")
        String number,

        @NotBlank(message = "Holder must not be blank")
        String holder,

        @NotNull(message = "Expiration date is required")
        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate
) {
}
