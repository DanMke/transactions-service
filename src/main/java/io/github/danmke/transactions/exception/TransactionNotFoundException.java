package io.github.danmke.transactions.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(Long transactionId) {
        super("Transaction " + transactionId + " not found");
    }
}
