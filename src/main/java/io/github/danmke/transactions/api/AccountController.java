package io.github.danmke.transactions.api;

import io.github.danmke.transactions.api.dto.AccountResponse;
import io.github.danmke.transactions.api.dto.CreateAccountRequest;
import io.github.danmke.transactions.application.AccountService;
import io.github.danmke.transactions.domain.Account;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.create(request.documentNumber());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(account.getId())
                .toUri();

        AccountResponse body = new AccountResponse(account.getId(), account.getDocumentNumber());
        return ResponseEntity.created(location).body(body);
    }
}
