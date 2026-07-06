package io.github.danmke.transactions.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(

        @NotBlank
        @Size(max = 50)
        String documentNumber
) {
}
