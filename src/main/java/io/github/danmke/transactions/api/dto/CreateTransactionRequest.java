package io.github.danmke.transactions.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransactionRequest(

        @NotNull
        Long accountId,

        @NotNull
        Integer operationTypeId,

        @NotNull
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount
) {
}
