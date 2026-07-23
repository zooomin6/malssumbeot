package com.malssumbeot.api;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Bean
    public ChatRateLimiter chatRateLimiter(
            @Value("${malssumbeot.chat.rate-limit.max-requests}") int maxRequests,
            @Value("${malssumbeot.chat.rate-limit.window}") Duration window) {
        return new ChatRateLimiter(maxRequests, window, Clock.systemUTC());
    }
}
