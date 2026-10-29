package com.collins.platform.payouts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.collins.platform.commons", "com.collins.platform.ledger.client",
        "com.collins.platform.payouts"})
public class PayoutsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayoutsApplication.class, args);
    }
}
