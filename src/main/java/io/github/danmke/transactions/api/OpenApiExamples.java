package io.github.danmke.transactions.api;

final class OpenApiExamples {

    private OpenApiExamples() {
    }

    static final String CREATE_ACCOUNT_REQUEST = """
            {
              "document_number": "12345678900"
            }""";

    static final String ACCOUNT_RESPONSE = """
            {
              "account_id": 1,
              "document_number": "12345678900"
            }""";

    static final String ACCOUNT_VALIDATION_ERROR = """
            {
              "type": "urn:problem-type:validation-failed",
              "title": "Validation failed",
              "status": 400,
              "detail": "Validation failed for one or more fields",
              "errors": [
                { "field": "document_number", "message": "must not be blank" }
              ]
            }""";

    static final String INVALID_REQUEST_PARAMETER = """
            {
              "type": "urn:problem-type:invalid-request-parameter",
              "title": "Invalid request parameter",
              "status": 400,
              "detail": "Request parameter has an invalid type"
            }""";

    static final String ACCOUNT_NOT_FOUND = """
            {
              "type": "urn:problem-type:account-not-found",
              "title": "Account not found",
              "status": 404,
              "detail": "Account 99999999 not found"
            }""";

    static final String CREATE_TRANSACTION_REQUEST = """
            {
              "account_id": 1,
              "operation_type_id": 1,
              "amount": 123.45
            }""";

    static final String TRANSACTION_RESPONSE = """
            {
              "transaction_id": 1,
              "account_id": 1,
              "operation_type_id": 1,
              "amount": -123.45,
              "event_date": "2026-07-06T12:00:00Z"
            }""";

    static final String TRANSACTION_LIST_RESPONSE = """
            [
              {
                "transaction_id": 2,
                "account_id": 1,
                "operation_type_id": 4,
                "amount": 60.00,
                "event_date": "2026-07-06T12:05:00Z"
              },
              {
                "transaction_id": 1,
                "account_id": 1,
                "operation_type_id": 1,
                "amount": -123.45,
                "event_date": "2026-07-06T12:00:00Z"
              }
            ]""";

    static final String TRANSACTION_VALIDATION_ERROR = """
            {
              "type": "urn:problem-type:validation-failed",
              "title": "Validation failed",
              "status": 400,
              "detail": "Validation failed for one or more fields",
              "errors": [
                { "field": "account_id", "message": "must not be null" }
              ]
            }""";

    static final String INVALID_OPERATION_TYPE = """
            {
              "type": "urn:problem-type:invalid-operation-type",
              "title": "Invalid operation type",
              "status": 422,
              "detail": "Operation type 99 does not exist"
            }""";

    static final String TRANSACTION_NOT_FOUND = """
            {
              "type": "urn:problem-type:transaction-not-found",
              "title": "Transaction not found",
              "status": 404,
              "detail": "Transaction 99999999 not found"
            }""";

    static final String MISSING_ACCOUNT_ID_PARAMETER = """
            {
              "type": "urn:problem-type:missing-request-parameter",
              "title": "Missing request parameter",
              "status": 400,
              "detail": "Required request parameter 'account_id' is missing"
            }""";
}
