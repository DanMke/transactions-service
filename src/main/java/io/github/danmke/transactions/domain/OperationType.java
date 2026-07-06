package io.github.danmke.transactions.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Code-level enum for the supported operation types. The {@code operation_types}
 * table exists only to back the foreign key on {@code transactions}; this enum is
 * NOT a queryable JPA entity and has no repository.
 */
public enum OperationType {

    NORMAL_PURCHASE(1, false),
    INSTALLMENT_PURCHASE(2, false),
    WITHDRAWAL(3, false),
    CREDIT_VOUCHER(4, true);

    private final int id;
    private final boolean positive;

    OperationType(int id, boolean positive) {
        this.id = id;
        this.positive = positive;
    }

    public int getId() {
        return id;
    }

    /**
     * Applies the correct sign to a monetary amount for this operation type,
     * always starting from the absolute value: purchases and withdrawals become
     * negative, credit vouchers become positive.
     */
    public BigDecimal normalize(BigDecimal amount) {
        BigDecimal magnitude = Objects.requireNonNull(amount, "Amount must not be null").abs();
        return positive ? magnitude : magnitude.negate();
    }

    public static OperationType fromId(int id) {
        for (OperationType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown operation type id: " + id);
    }
}
