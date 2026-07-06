package io.github.danmke.transactions.exception;

/**
 * The {@code amount} is well-formed and positive in the payload, but zero is
 * not a valid transaction amount. This is a business-rule violation (422),
 * not a payload error.
 */
public class InvalidTransactionAmountException extends RuntimeException {

    public InvalidTransactionAmountException() {
        super("Amount must not be zero");
    }
}
