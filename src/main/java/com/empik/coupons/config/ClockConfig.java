package com.empik.coupons.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    /** Injected instead of calling Instant.now() so that time is controllable in tests. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
