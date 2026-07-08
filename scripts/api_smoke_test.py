#!/usr/bin/env python3

import argparse
import json
import os
import sys
import time
import uuid
from decimal import Decimal
from urllib.error import HTTPError, URLError
from urllib.parse import urljoin
from urllib.request import Request, urlopen


class SmokeTestFailure(AssertionError):
    pass


def parse_args():
    parser = argparse.ArgumentParser(
        description="Run API smoke tests against transactions-service."
    )
    parser.add_argument(
        "--base-url",
        default=os.getenv("API_BASE_URL", "http://localhost:8080"),
        help="Base URL of the service. Default: API_BASE_URL or http://localhost:8080.",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=float(os.getenv("API_TEST_TIMEOUT", "5")),
        help="HTTP timeout in seconds. Default: API_TEST_TIMEOUT or 5.",
    )
    parser.add_argument(
        "--iterations",
        type=int,
        default=int(os.getenv("API_TEST_ITERATIONS", "1")),
        help="Number of happy-path account/transaction cycles. Default: API_TEST_ITERATIONS or 1.",
    )
    parser.add_argument(
        "--amount",
        default=os.getenv("API_TEST_AMOUNT", "123.45"),
        help="Positive transaction amount used in happy-path tests. Default: API_TEST_AMOUNT or 123.45.",
    )
    parser.add_argument(
        "--document-prefix",
        default=os.getenv("API_TEST_DOCUMENT_PREFIX", "smoke"),
        help="Prefix used to generate unique document numbers. Default: API_TEST_DOCUMENT_PREFIX or smoke.",
    )
    parser.add_argument(
        "--purchase-operation-type-id",
        type=int,
        default=int(os.getenv("API_TEST_PURCHASE_OPERATION_TYPE_ID", "1")),
        help="Operation type expected to store a negative amount. Default: 1.",
    )
    parser.add_argument(
        "--credit-operation-type-id",
        type=int,
        default=int(os.getenv("API_TEST_CREDIT_OPERATION_TYPE_ID", "4")),
        help="Operation type expected to store a positive amount. Default: 4.",
    )
    parser.add_argument(
        "--skip-error-cases",
        action="store_true",
        default=os.getenv("API_TEST_SKIP_ERROR_CASES", "false").lower() == "true",
        help="Skip validation/business error checks.",
    )
    parser.add_argument(
        "--skip-observability",
        action="store_true",
        default=os.getenv("API_TEST_SKIP_OBSERVABILITY", "false").lower() == "true",
        help="Skip /actuator/prometheus checks.",
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        default=os.getenv("API_TEST_VERBOSE", "false").lower() == "true",
        help="Print response bodies.",
    )
    return parser.parse_args()


def normalize_base_url(base_url):
    return base_url.rstrip("/") + "/"


def request_json(base_url, method, path, timeout, payload=None, expected_status=None, verbose=False):
    url = urljoin(base_url, path.lstrip("/"))
    body = None
    headers = {"Accept": "application/json"}

    if payload is not None:
        body = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"

    request = Request(url, data=body, headers=headers, method=method)

    try:
        with urlopen(request, timeout=timeout) as response:
            status = response.status
            response_body = response.read().decode("utf-8")
            content_type = response.headers.get("Content-Type", "")
    except HTTPError as error:
        status = error.code
        response_body = error.read().decode("utf-8")
        content_type = error.headers.get("Content-Type", "")
    except URLError as error:
        raise SmokeTestFailure(f"{method} {url} failed: {error}") from error

    if verbose:
        print(f"{method} {url} -> {status}")
        if response_body:
            print(response_body)

    if expected_status is not None and status != expected_status:
        raise SmokeTestFailure(
            f"{method} {url} expected HTTP {expected_status}, got {status}. Body: {response_body}"
        )

    if not response_body:
        return status, None, content_type

    try:
        return status, json.loads(response_body, parse_float=Decimal), content_type
    except json.JSONDecodeError:
        return status, response_body, content_type


def request_text(base_url, method, path, timeout, expected_status=None, verbose=False):
    url = urljoin(base_url, path.lstrip("/"))
    request = Request(url, headers={"Accept": "*/*"}, method=method)

    try:
        with urlopen(request, timeout=timeout) as response:
            status = response.status
            response_body = response.read().decode("utf-8")
    except HTTPError as error:
        status = error.code
        response_body = error.read().decode("utf-8")
    except URLError as error:
        raise SmokeTestFailure(f"{method} {url} failed: {error}") from error

    if verbose:
        print(f"{method} {url} -> {status}")
        if response_body:
            print(response_body)

    if expected_status is not None and status != expected_status:
        raise SmokeTestFailure(
            f"{method} {url} expected HTTP {expected_status}, got {status}. Body: {response_body}"
        )

    return status, response_body


def check(condition, message):
    if not condition:
        raise SmokeTestFailure(message)


def expect_problem(body, problem_type):
    check(isinstance(body, dict), f"Expected ProblemDetail object, got {body!r}")
    check(
        body.get("type") == f"urn:problem-type:{problem_type}",
        f"Expected problem type {problem_type}, got {body.get('type')}",
    )


def generated_document(prefix, index):
    suffix = f"{int(time.time() * 1000)}{index}{uuid.uuid4().hex[:8]}"
    return f"{prefix}{suffix}"[:50]


def decimal_from_json(value):
    return value if isinstance(value, Decimal) else Decimal(str(value))


def run_health_check(args):
    _, body, _ = request_json(
        args.base_url, "GET", "/actuator/health", args.timeout, expected_status=200, verbose=args.verbose
    )
    check(body.get("status") == "UP", f"Expected health status UP, got {body}")
    print("[OK] health endpoint is UP")


def create_account(args, document_number):
    _, body, content_type = request_json(
        args.base_url,
        "POST",
        "/accounts",
        args.timeout,
        payload={"document_number": document_number},
        expected_status=201,
        verbose=args.verbose,
    )
    check("application/json" in content_type, f"Expected JSON response, got {content_type}")
    check(body.get("account_id") is not None, f"Missing account_id in {body}")
    check(body.get("document_number") == document_number, f"Unexpected account response {body}")
    print(f"[OK] created account {body['account_id']}")
    return body


def get_account(args, account_id, expected_document):
    _, body, _ = request_json(
        args.base_url,
        "GET",
        f"/accounts/{account_id}",
        args.timeout,
        expected_status=200,
        verbose=args.verbose,
    )
    check(body.get("account_id") == account_id, f"Unexpected account_id in {body}")
    check(body.get("document_number") == expected_document, f"Unexpected document_number in {body}")
    print(f"[OK] fetched account {account_id}")


def create_transaction(args, account_id, operation_type_id, amount, expected_amount):
    _, body, _ = request_json(
        args.base_url,
        "POST",
        "/transactions",
        args.timeout,
        payload={
            "account_id": account_id,
            "operation_type_id": operation_type_id,
            "amount": amount,
        },
        expected_status=201,
        verbose=args.verbose,
    )
    actual_amount = decimal_from_json(body.get("amount"))
    check(body.get("transaction_id") is not None, f"Missing transaction_id in {body}")
    check(body.get("account_id") == account_id, f"Unexpected account_id in {body}")
    check(body.get("operation_type_id") == operation_type_id, f"Unexpected operation_type_id in {body}")
    check(actual_amount == expected_amount, f"Expected amount {expected_amount}, got {actual_amount}")
    check(body.get("event_date") is not None, f"Missing event_date in {body}")
    print(f"[OK] created transaction {body['transaction_id']} amount={actual_amount}")
    return body


def get_transaction(args, transaction_id, expected_amount):
    _, body, _ = request_json(
        args.base_url,
        "GET",
        f"/transactions/{transaction_id}",
        args.timeout,
        expected_status=200,
        verbose=args.verbose,
    )
    check(body.get("transaction_id") == transaction_id, f"Unexpected transaction_id in {body}")
    actual_amount = decimal_from_json(body.get("amount"))
    check(actual_amount == expected_amount, f"Expected amount {expected_amount}, got {actual_amount}")
    print(f"[OK] fetched transaction {transaction_id}")


def list_transactions(args, account_id, expected_transaction_ids):
    _, body, _ = request_json(
        args.base_url,
        "GET",
        f"/transactions?account_id={account_id}",
        args.timeout,
        expected_status=200,
        verbose=args.verbose,
    )
    check(isinstance(body, list), f"Expected transaction list, got {body!r}")
    actual_transaction_ids = {item.get("transaction_id") for item in body}
    missing_transaction_ids = set(expected_transaction_ids) - actual_transaction_ids
    check(
        not missing_transaction_ids,
        f"Missing transactions for account {account_id}: {sorted(missing_transaction_ids)}. Body: {body}",
    )
    print(f"[OK] listed {len(body)} transaction(s) for account {account_id}")


def run_happy_path(args):
    amount = Decimal(args.amount)
    check(amount > 0, "--amount must be positive")

    for index in range(1, args.iterations + 1):
        document_number = generated_document(args.document_prefix, index)
        account = create_account(args, document_number)
        account_id = account["account_id"]

        get_account(args, account_id, document_number)
        purchase_transaction = create_transaction(
            args,
            account_id,
            args.purchase_operation_type_id,
            str(amount),
            -abs(amount),
        )
        credit_transaction = create_transaction(
            args,
            account_id,
            args.credit_operation_type_id,
            str(amount),
            abs(amount),
        )
        get_transaction(
            args,
            purchase_transaction["transaction_id"],
            -abs(amount),
        )
        list_transactions(
            args,
            account_id,
            [purchase_transaction["transaction_id"], credit_transaction["transaction_id"]],
        )


def run_error_cases(args):
    _, body, _ = request_json(
        args.base_url,
        "POST",
        "/accounts",
        args.timeout,
        payload={},
        expected_status=400,
        verbose=args.verbose,
    )
    expect_problem(body, "validation-failed")
    print("[OK] account validation error returns ProblemDetail")

    _, body, _ = request_json(
        args.base_url,
        "GET",
        "/accounts/99999999",
        args.timeout,
        expected_status=404,
        verbose=args.verbose,
    )
    expect_problem(body, "account-not-found")
    print("[OK] missing account returns 404 ProblemDetail")

    _, body, _ = request_json(
        args.base_url,
        "GET",
        "/transactions/99999999",
        args.timeout,
        expected_status=404,
        verbose=args.verbose,
    )
    expect_problem(body, "transaction-not-found")
    print("[OK] missing transaction returns 404 ProblemDetail")

    document_number = generated_document(args.document_prefix, 999)
    account = create_account(args, document_number)
    account_id = account["account_id"]

    _, body, _ = request_json(
        args.base_url,
        "POST",
        "/transactions",
        args.timeout,
        payload={"account_id": account_id, "operation_type_id": 99, "amount": "10.00"},
        expected_status=422,
        verbose=args.verbose,
    )
    expect_problem(body, "invalid-operation-type")
    print("[OK] invalid operation type returns 422 ProblemDetail")

    _, body, _ = request_json(
        args.base_url,
        "POST",
        "/transactions",
        args.timeout,
        payload={"account_id": account_id, "operation_type_id": args.purchase_operation_type_id, "amount": "0.00"},
        expected_status=422,
        verbose=args.verbose,
    )
    expect_problem(body, "invalid-transaction-amount")
    print("[OK] zero amount returns 422 ProblemDetail")

    _, body, _ = request_json(
        args.base_url,
        "POST",
        "/transactions",
        args.timeout,
        payload={"account_id": account_id, "operation_type_id": args.purchase_operation_type_id, "amount": "-10.00"},
        expected_status=400,
        verbose=args.verbose,
    )
    expect_problem(body, "negative-amount")
    print("[OK] negative amount returns 400 ProblemDetail")


def run_observability_check(args):
    _, body = request_text(
        args.base_url,
        "GET",
        "/actuator/prometheus",
        args.timeout,
        expected_status=200,
        verbose=args.verbose,
    )
    for metric in ("accounts_creation_total", "transactions_creation_total", "transactions_failed_total"):
        check(metric in body, f"Expected metric {metric} in /actuator/prometheus")
    print("[OK] prometheus endpoint exposes custom business metrics")


def main():
    args = parse_args()
    args.base_url = normalize_base_url(args.base_url)

    if args.iterations < 1:
        raise SmokeTestFailure("--iterations must be greater than zero")

    print(f"Running smoke tests against {args.base_url}")
    run_health_check(args)
    run_happy_path(args)

    if not args.skip_error_cases:
        run_error_cases(args)

    if not args.skip_observability:
        run_observability_check(args)

    print("Smoke tests finished successfully.")


if __name__ == "__main__":
    try:
        main()
    except SmokeTestFailure as error:
        print(f"[FAIL] {error}", file=sys.stderr)
        sys.exit(1)
    except KeyboardInterrupt:
        print("[FAIL] interrupted", file=sys.stderr)
        sys.exit(130)
