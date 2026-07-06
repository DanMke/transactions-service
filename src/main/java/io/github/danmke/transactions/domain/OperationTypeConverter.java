package io.github.danmke.transactions.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps the {@link OperationType} enum to/from the {@code operation_type_id}
 * foreign key column, so the enum stays a plain code artifact while the DB keeps
 * a real FK to {@code operation_types}.
 */
@Converter(autoApply = false)
public class OperationTypeConverter implements AttributeConverter<OperationType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(OperationType attribute) {
        return attribute == null ? null : attribute.getId();
    }

    @Override
    public OperationType convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : OperationType.fromId(dbData);
    }
}
