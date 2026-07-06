package io.github.danmke.transactions.api.dto;

import io.github.danmke.transactions.domain.Transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionResponse(
        Long transactionId,
        Long accountId,
        Integer operationTypeId,
        BigDecimal amount,
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
