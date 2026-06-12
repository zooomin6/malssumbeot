package com.malssumbeot.crisis;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class CrisisConfig {

    @Bean
    public CrisisDetector crisisDetector() {
        try {
            String content = new ClassPathResource("crisis/crisis-patterns.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
            return new CrisisDetector(content.lines().toList());
        } catch (IOException e) {
            throw new UncheckedIOException("위기 패턴 파일을 읽을 수 없습니다", e);
        }
    }

    @Bean
    public CrisisSessionStore crisisSessionStore(
            @Value("${malssumbeot.crisis.sticky-duration}") Duration stickyDuration) {
        return new CrisisSessionStore(stickyDuration, Clock.systemUTC());
    }

    @Bean
    public CrisisFilter crisisFilter(CrisisDetector detector, CrisisSessionStore sessionStore) {
        return new CrisisFilter(detector, sessionStore);
    }
}
