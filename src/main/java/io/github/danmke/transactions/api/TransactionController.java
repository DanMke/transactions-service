package io.github.danmke.transactions.api;

import io.github.danmke.transactions.api.dto.CreateTransactionRequest;
import io.github.danmke.transactions.api.dto.TransactionResponse;
import io.github.danmke.transactions.application.TransactionService;
import io.github.danmke.transactions.domain.Transaction;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request) {
        Transaction transaction = transactionService.create(
                request.accountId(), request.operationTypeId(), request.amount());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(transaction.getId())
                .toUri();

        return ResponseEntity.created(location).body(TransactionResponse.from(transaction));
    }
}
