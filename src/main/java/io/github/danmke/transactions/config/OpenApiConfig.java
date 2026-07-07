package io.github.danmke.transactions.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transactionsOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Transactions Service API")
                .version("0.0.1")
                .description("""
                        REST API for managing accounts and financial transactions. \
                        Each operation type normalizes the amount sign: purchases and \
                        withdrawals become negative, credit vouchers positive."""));
    }
}
