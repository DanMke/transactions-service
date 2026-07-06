CREATE TABLE operation_types (
    operation_type_id INTEGER PRIMARY KEY,
    description        VARCHAR(50) NOT NULL
);

INSERT INTO operation_types (operation_type_id, description) VALUES
    (1, 'NORMAL_PURCHASE'),
    (2, 'INSTALLMENT_PURCHASE'),
    (3, 'WITHDRAWAL'),
    (4, 'CREDIT_VOUCHER');
