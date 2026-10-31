package com.innoboard.userservice.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(

        @NotNull(message = "Active flag is required")
        Boolean active
) {
}
