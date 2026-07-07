package io.github.danmke.transactions.observability;

import io.github.danmke.transactions.domain.OperationType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void accountCreated() {
        Counter.builder("accounts.creation")
                .description("Number of accounts created")
                .register(meterRegistry)
                .increment();
    }

    public void transactionCreated(OperationType operationType) {
        Counter.builder("transactions.creation")
                .description("Number of transactions created")
                .tag("operation_type", operationType.name())
                .register(meterRegistry)
                .increment();
    }

    public void transactionFailed(String reason) {
        Counter.builder("transactions.failed")
                .description("Number of transaction creation failures")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }
}
