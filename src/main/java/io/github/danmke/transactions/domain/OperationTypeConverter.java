package io.github.danmke.transactions.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

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
