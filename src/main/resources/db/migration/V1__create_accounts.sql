CREATE TABLE accounts (
    account_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_number VARCHAR(50) NOT NULL,

    CONSTRAINT ck_accounts_document_number_not_blank CHECK (btrim(document_number) <> '')
);
