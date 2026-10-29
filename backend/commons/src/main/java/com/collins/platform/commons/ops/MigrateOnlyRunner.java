package com.collins.platform.commons.ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * When {@code platform.migrate-only=true}, the app starts (which runs Flyway), then exits with code 0.
 * Kubernetes runs this as a Job before rolling out a new version, so replicas never race to migrate.
 */
@Component
@ConditionalOnProperty(name = "platform.migrate-only", havingValue = "true")
public class MigrateOnlyRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrateOnlyRunner.class);

    private final ApplicationContext context;

    public MigrateOnlyRunner(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Migrations applied; exiting because platform.migrate-only=true");
        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
