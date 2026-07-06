package io.github.danmke.transactions.exception;

/**
 * The supplied {@code operation_type_id} does not correspond to any known
 * operation type. This is a business-rule violation (422): the payload is
 * well-formed, but references a value the domain cannot process.
 */
public class InvalidOperationTypeException extends RuntimeException {

    public InvalidOperationTypeException(Integer operationTypeId) {
        super("Operation type " + operationTypeId + " does not exist");
    }
}
