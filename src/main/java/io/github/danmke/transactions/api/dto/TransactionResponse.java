package io.github.danmke.transactions.api.dto;

import io.github.danmke.transactions.domain.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionResponse(

        @Schema(example = "1")
        Long transactionId,

        @Schema(example = "1")
        Long accountId,

        @Schema(example = "4")
        Integer operationTypeId,

        @Schema(example = "60.00", description = "Amount with the sign already applied by the operation type")
        BigDecimal amount,

        @Schema(example = "2026-07-06T12:00:00Z")
        OffsetDateTime eventDate
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getOperationType().getId(),
                transaction.getAmount(),
                transaction.getEventDate()
        );
    }
}
