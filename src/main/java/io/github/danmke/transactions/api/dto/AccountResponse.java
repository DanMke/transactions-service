package io.github.danmke.transactions.api.dto;

import io.github.danmke.transactions.domain.Account;
import io.swagger.v3.oas.annotations.media.Schema;

public record AccountResponse(

        @Schema(example = "1")
        Long accountId,

        @Schema(example = "12345678900")
        String documentNumber
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getDocumentNumber());
    }
}
