package io.github.danmke.transactions.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(

        @Schema(example = "12345678900")
        @NotBlank
        @Size(max = 50)
        String documentNumber
) {
}
