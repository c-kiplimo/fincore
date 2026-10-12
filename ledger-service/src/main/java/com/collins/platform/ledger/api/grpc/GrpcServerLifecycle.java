package com.collins.platform.ledger.api.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Starts and stops the gRPC server together with the Spring context. */
@Component
@ConditionalOnProperty(name = "ledger.grpc.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcServerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerLifecycle.class);

    private final LedgerGrpcService service;
    private final int port;
    private Server server;

    public GrpcServerLifecycle(LedgerGrpcService service, @Value("${ledger.grpc.port:9091}") int port) {
        this.service = service;
        this.port = port;
    }

    @Override
    public void start() {
        try {
            server = ServerBuilder.forPort(port)
                    .addService(service)
                    .addService(ProtoReflectionService.newInstance())
                    .executor(Executors.newVirtualThreadPerTaskExecutor())
                    .build()
                    .start();
            log.info("ledger.v1 gRPC server listening on {}", port);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot start gRPC server", e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
            try {
                server.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return server != null && !server.isShutdown();
    }
}
