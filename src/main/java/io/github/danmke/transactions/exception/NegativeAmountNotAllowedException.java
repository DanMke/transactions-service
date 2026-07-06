package io.github.danmke.transactions.exception;

import java.math.BigDecimal;

/**
 * The API only accepts a positive magnitude for {@code amount}; the sign is
 * derived from the operation type. Sending a negative value is a client
 * contract error (400), not a business-rule violation.
 */
public class NegativeAmountNotAllowedException extends RuntimeException {

    public NegativeAmountNotAllowedException(BigDecimal amount) {
        super("Amount must be a positive magnitude, but was " + amount.toPlainString());
    }
}
