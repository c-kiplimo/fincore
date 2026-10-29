package com.collins.platform.ledger.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the gRPC channel and {@link LedgerClient} in services that set
 * {@code platform.ledger.grpc-target} (e.g. {@code ledger-service:9091}).
 */
@Configuration
@ConditionalOnProperty(name = "platform.ledger.grpc-target")
public class LedgerClientConfiguration {

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel ledgerChannel(@Value("${platform.ledger.grpc-target}") String target) {
        return ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    }

    @Bean
    public LedgerClient ledgerClient(ManagedChannel ledgerChannel) {
        return new LedgerClient(ledgerChannel);
    }
}
