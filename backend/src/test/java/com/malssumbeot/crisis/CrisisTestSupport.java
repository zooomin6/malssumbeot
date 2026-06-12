package com.malssumbeot.crisis;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.core.io.ClassPathResource;

final class CrisisTestSupport {

    /** 운영과 동일한 패턴 파일로 테스트한다 — 픽스처가 실물과 어긋나는 것을 방지. */
    static CrisisDetector productionDetector() {
        try {
            String content = new ClassPathResource("crisis/crisis-patterns.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
            return new CrisisDetector(content.lines().toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 테스트에서 시간을 임의로 흘릴 수 있는 가변 시계. */
    static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-06-12T00:00:00Z");

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private CrisisTestSupport() {
    }
}
