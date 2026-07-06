package io.github.danmke.transactions.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    private static final OffsetDateTime EVENT_DATE = OffsetDateTime.parse("2026-07-06T12:00:00Z");

    @Test
    void createsTransactionWithAmountNormalizedByOperationType() {
        Transaction transaction = new Transaction(
                1L,
                OperationType.NORMAL_PURCHASE,
                new BigDecimal("123.45"),
                EVENT_DATE
        );

        assertThat(transaction.getAmount()).isEqualByComparingTo("-123.45");
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> new Transaction(
                1L,
                OperationType.CREDIT_VOUCHER,
                BigDecimal.ZERO,
                EVENT_DATE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must not be null or zero");
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> new Transaction(
                1L,
                OperationType.NORMAL_PURCHASE,
                null,
                EVENT_DATE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must not be null or zero");
    }
}
