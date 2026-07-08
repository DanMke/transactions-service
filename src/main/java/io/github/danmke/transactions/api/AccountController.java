package io.github.danmke.transactions.api;

import io.github.danmke.transactions.api.dto.AccountResponse;
import io.github.danmke.transactions.api.dto.CreateAccountRequest;
import io.github.danmke.transactions.application.AccountService;
import io.github.danmke.transactions.domain.Account;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Accounts", description = "Account creation and lookup")
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "Create an account")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(implementation = CreateAccountRequest.class),
                    examples = @ExampleObject(name = "Create account request",
                            value = OpenApiExamples.CREATE_ACCOUNT_REQUEST)))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class),
                            examples = @ExampleObject(name = "Account created",
                                    value = OpenApiExamples.ACCOUNT_RESPONSE))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Validation error",
                                    value = OpenApiExamples.ACCOUNT_VALIDATION_ERROR)))
    })
    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.create(request.documentNumber());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(account.getId())
                .toUri();

        return ResponseEntity.created(location).body(AccountResponse.from(account));
    }

    @Operation(summary = "Get an account by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class),
                            examples = @ExampleObject(name = "Account found",
                                    value = OpenApiExamples.ACCOUNT_RESPONSE))),
            @ApiResponse(responseCode = "400", description = "Invalid account id",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Invalid account id",
                                    value = OpenApiExamples.INVALID_REQUEST_PARAMETER))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(name = "Account not found",
                                    value = OpenApiExamples.ACCOUNT_NOT_FOUND)))
    })
    @GetMapping("/{accountId}")
    public AccountResponse getById(@PathVariable Long accountId) {
        return AccountResponse.from(accountService.getById(accountId));
    }
}
