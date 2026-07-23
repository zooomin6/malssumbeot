package com.malssumbeot.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자별 채팅 요청 횟수 제한 (비용 폭주 방지). 고정 윈도 방식 — 윈도 시작 후 경과 시간이
 * windowDuration을 넘으면 카운트를 리셋한다.
 *
 * 현재는 인메모리 구현 — 다중 인스턴스로 스케일하면 Redis 등 공유 저장소로 옮긴다.
 */
public class ChatRateLimiter {

    private record Window(int count, Instant start) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final Duration windowDuration;
    private final Clock clock;

    public ChatRateLimiter(int maxRequests, Duration windowDuration, Clock clock) {
        this.maxRequests = maxRequests;
        this.windowDuration = windowDuration;
        this.clock = clock;
    }

    /** 이번 요청을 허용하면 true, 윈도 내 한도를 넘었으면 false. */
    public boolean tryAcquire(String userId) {
        Instant now = clock.instant();
        boolean[] allowed = {true};
        windows.compute(userId, (id, existing) -> {
            if (existing == null || Duration.between(existing.start(), now).compareTo(windowDuration) > 0) {
                return new Window(1, now);
            }
            if (existing.count() >= maxRequests) {
                allowed[0] = false;
                return existing;
            }
            return new Window(existing.count() + 1, existing.start());
        });
        return allowed[0];
    }
}
