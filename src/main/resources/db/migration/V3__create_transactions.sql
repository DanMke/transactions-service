CREATE TABLE transactions (
    transaction_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id        BIGINT NOT NULL REFERENCES accounts (account_id),
    operation_type_id INTEGER NOT NULL REFERENCES operation_types (operation_type_id),
    amount            NUMERIC(19, 2) NOT NULL,
    event_date        TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_transactions_amount_not_zero CHECK (amount <> 0)
);

CREATE INDEX idx_transactions_account_id ON transactions (account_id);
