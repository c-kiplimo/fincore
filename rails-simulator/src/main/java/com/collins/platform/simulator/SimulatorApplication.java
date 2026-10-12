package com.collins.platform.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Stands in for the outside world: card network, mobile-money provider, bank and FX feed.
 * Only the web parts of commons are scanned: the simulator has no database and no Kafka.
 */
@SpringBootApplication(scanBasePackages = {"com.collins.platform.commons.web", "com.collins.platform.simulator"})
public class SimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
    }
}
