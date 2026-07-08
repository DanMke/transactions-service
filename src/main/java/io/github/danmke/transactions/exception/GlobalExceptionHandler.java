package io.github.danmke.transactions.exception;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Account not found", ex.getMessage(), "account-not-found");
    }

    @ExceptionHandler(NegativeAmountNotAllowedException.class)
    public ProblemDetail handleNegativeAmount(NegativeAmountNotAllowedException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Negative amount not allowed", ex.getMessage(), "negative-amount");
    }

    @ExceptionHandler(InvalidOperationTypeException.class)
    public ProblemDetail handleInvalidOperationType(InvalidOperationTypeException ex) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Invalid operation type",
                ex.getMessage(),
                "invalid-operation-type");
    }

    @ExceptionHandler(InvalidTransactionAmountException.class)
    public ProblemDetail handleInvalidTransactionAmount(InvalidTransactionAmountException ex) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Invalid transaction amount",
                ex.getMessage(),
                "invalid-transaction-amount");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "Validation failed for one or more fields",
                "validation-failed");

        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationError(toSnakeCase(fieldError.getField()), fieldError.getDefaultMessage()))
                .toList();
        problem.setProperty("errors", errors);

        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request",
                "Request body is malformed or cannot be parsed",
                "malformed-json");

        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameter",
                "Request parameter has an invalid type",
                "invalid-request-parameter");

        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Missing request parameter",
                "Required request parameter '" + ex.getParameterName() + "' is missing",
                "missing-request-parameter");

        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("urn:problem-type:" + type));
        return problem;
    }

    private String toSnakeCase(String fieldName) {
        return fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private record ValidationError(String field, String message) {
    }
}
