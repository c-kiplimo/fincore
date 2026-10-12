package com.collins.platform.commons;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on scheduling for the outbox relay and each service's jobs. */
@Configuration
@EnableScheduling
public class CommonsConfiguration {
}
