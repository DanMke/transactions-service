package io.github.danmke.transactions.exception;

public class InvalidTransactionAmountException extends RuntimeException {

    public InvalidTransactionAmountException() {
        super("Amount must not be zero");
    }
}
