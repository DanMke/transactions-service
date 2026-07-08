package io.github.danmke.transactions.exception;

import java.math.BigDecimal;

public class NegativeAmountNotAllowedException extends RuntimeException {

    public NegativeAmountNotAllowedException(BigDecimal amount) {
        super("Amount must be a positive magnitude, but was " + amount.toPlainString());
    }
}
