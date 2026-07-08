package io.github.danmke.transactions.api;

import io.github.danmke.transactions.api.dto.CreateTransactionRequest;
import io.github.danmke.transactions.api.dto.TransactionResponse;
import io.github.danmke.transactions.application.TransactionService;
import io.github.danmke.transactions.domain.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Transactions", description = "Transaction creation and account-scoped lookup")
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "Create a transaction",
            description = "Amount must be a positive magnitude; the sign is derived from the operation type.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(implementation = CreateTransactionRequest.class),
                    examples = @ExampleObject(name = "Create transaction request",
                            value = OpenApiExamples.CREATE_TRANSACTION_REQUEST)))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class),
                            examples = @ExampleObject(name = "Transaction created",
                                    value = OpenApiExamples.TRANSACTION_RESPONSE))),
            @ApiResponse(responseCode = "400", description = "Invalid payload (missing/oversized fields) or negative amount",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Invalid payload",
                                    value = OpenApiExamples.TRANSACTION_VALIDATION_ERROR))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Account not found",
                                    value = OpenApiExamples.ACCOUNT_NOT_FOUND))),
            @ApiResponse(responseCode = "422", description = "Unknown operation type or zero amount",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Invalid operation type",
                                    value = OpenApiExamples.INVALID_OPERATION_TYPE)))
    })
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

    @Operation(summary = "List transactions by account",
            description = "Returns transactions for the given account_id, ordered by newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class)),
                            examples = @ExampleObject(name = "Transaction list",
                                    value = OpenApiExamples.TRANSACTION_LIST_RESPONSE))),
            @ApiResponse(responseCode = "400", description = "Invalid account id",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Invalid account id",
                                    value = OpenApiExamples.INVALID_ACCOUNT_ID))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Account not found",
                                    value = OpenApiExamples.ACCOUNT_NOT_FOUND)))
    })
    @GetMapping
    public List<TransactionResponse> list(
            @Parameter(description = "Account id used to filter transactions", example = "1", required = true)
            @RequestParam(name = "account_id") Long accountId) {
        return transactionService.listByAccountId(accountId).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
