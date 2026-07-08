package io.github.danmke.transactions.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransactionRequest(

        @Schema(example = "1")
        @NotNull
        Long accountId,

        @Schema(example = "4", description = "1=NORMAL_PURCHASE, 2=INSTALLMENT_PURCHASE, 3=WITHDRAWAL, 4=CREDIT_VOUCHER")
        @NotNull
        Integer operationTypeId,

        @Schema(example = "123.45", description = "Positive magnitude; the sign is applied from the operation type")
        @NotNull
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount
) {
}
