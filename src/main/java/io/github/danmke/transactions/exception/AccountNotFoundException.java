package io.github.danmke.transactions.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long accountId) {
        super("Account " + accountId + " not found");
    }
}
