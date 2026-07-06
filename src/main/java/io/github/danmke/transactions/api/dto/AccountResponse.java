package io.github.danmke.transactions.api.dto;

import io.github.danmke.transactions.domain.Account;

public record AccountResponse(Long accountId, String documentNumber) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getDocumentNumber());
    }
}
