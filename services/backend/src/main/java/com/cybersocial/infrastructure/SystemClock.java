package com.cybersocial.infrastructure;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemClock {

    @Bean
    public Clock utcClock() {
        return Clock.systemUTC();
    }
}
