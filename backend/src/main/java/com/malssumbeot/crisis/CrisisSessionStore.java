package com.malssumbeot.crisis;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 세션 단위 1회성 위기 sticky 상태 (D-026, D-020의 시간 기반 하강 대체).
 *
 * 위기 신호가 감지된 바로 다음 메시지에 "딱 한 번만" sticky 처리를 적용한다. 그 메시지에도
 * 위기 패턴이 없으면(CrisisFilter가 새 신호로 재마킹하지 않는 한) 즉시 위기 모드에서 빠져나와
 * 사용자의 새 요청을 그대로 반영한다 — 시간이 지나며 서서히 누그러뜨리던 이전 방식
 * (HIGH→MID→LOW→NONE)은 여기서 폐기되었다(민규 결정, 2026-07-22).
 *
 * markedAt은 응답 문구 선택에는 쓰이지 않는다. 세션이 끝난 뒤 다시 돌아오지 않아 항목이
 * 무기한 남는 것을 막는 정리(@Scheduled)에만 쓰인다.
 */
public class CrisisSessionStore {

    private static final Duration STALE_AFTER = Duration.ofHours(24);

    /** consumeSticky가 돌려주는 값. category는 null일 수 있다(2차 방어선 판정). */
    public record CrisisMark(String category) {
    }

    private record InternalMark(String category, Instant markedAt) {
    }

    private final Map<String, InternalMark> marks = new ConcurrentHashMap<>();
    private final Clock clock;

    public CrisisSessionStore() {
        this(Clock.systemUTC());
    }

    CrisisSessionStore(Clock clock) {
        this.clock = clock;
    }

    /** 위기 신호 감지 시 호출. category는 null 허용(2차 방어선 판정). */
    public void mark(String sessionId, String category) {
        marks.put(sessionId, new InternalMark(category, clock.instant()));
    }

    /** sticky를 딱 한 번만 소비한다 — 호출 즉시 항목이 사라진다(1회성, D-026). */
    public Optional<CrisisMark> consumeSticky(String sessionId) {
        InternalMark mark = marks.remove(sessionId);
        return mark == null ? Optional.empty() : Optional.of(new CrisisMark(mark.category()));
    }

    /** 다음 메시지 없이 방치된 세션 정리 — 응답 문구 선택과는 무관한 순수 메모리 관리다. */
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    void evictStale() {
        Instant cutoff = clock.instant().minus(STALE_AFTER);
        marks.values().removeIf(mark -> mark.markedAt().isBefore(cutoff));
    }
}
