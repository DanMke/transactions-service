package io.github.danmke.transactions.exception;

public class InvalidOperationTypeException extends RuntimeException {

    public InvalidOperationTypeException(Integer operationTypeId) {
        super("Operation type " + operationTypeId + " does not exist");
    }
}
