package io.github.danmke.transactions.api;

import io.github.danmke.transactions.api.dto.CreateTransactionRequest;
import io.github.danmke.transactions.api.dto.TransactionResponse;
import io.github.danmke.transactions.application.TransactionService;
import io.github.danmke.transactions.domain.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Transactions", description = "Transaction creation")
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "Create a transaction",
            description = "Amount must be a positive magnitude; the sign is derived from the operation type.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = CreateTransactionRequest.class),
                    examples = @ExampleObject(name = "Create transaction request", value = """
                            {
                              "account_id": 1,
                              "operation_type_id": 1,
                              "amount": 123.45
                            }
                            """)))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created",
                    content = @Content(
                            schema = @Schema(implementation = TransactionResponse.class),
                            examples = @ExampleObject(name = "Transaction created", value = """
                                    {
                                      "transaction_id": 1,
                                      "account_id": 1,
                                      "operation_type_id": 1,
                                      "amount": -123.45,
                                      "event_date": "2026-07-06T12:00:00Z"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid payload (missing/oversized fields) or negative amount",
                    content = @Content(
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Invalid payload", value = """
                                    {
                                      "type": "urn:problem-type:validation-failed",
                                      "title": "Validation failed",
                                      "status": 400,
                                      "detail": "Validation failed for one or more fields",
                                      "errors": [
                                        {
                                          "field": "accountId",
                                          "message": "must not be null"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Account not found", value = """
                                    {
                                      "type": "urn:problem-type:account-not-found",
                                      "title": "Account not found",
                                      "status": 404,
                                      "detail": "Account 99999999 not found"
                                    }
                                    """))),
            @ApiResponse(responseCode = "422", description = "Unknown operation type or zero amount",
                    content = @Content(
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Invalid operation type", value = """
                                    {
                                      "type": "urn:problem-type:invalid-operation-type",
                                      "title": "Invalid operation type",
                                      "status": 422,
                                      "detail": "Operation type 99 does not exist"
                                    }
                                    """)))
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
}
