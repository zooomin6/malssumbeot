package com.malssumbeot.crisis;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 세션 단위 sticky 위기 상태 (theology-checker 2026-06-12 지적 반영).
 *
 * 직전 턴에 위기 신호가 있었다면, 이후 턴이 평범한 내용("면접 기도문 써줘")이어도
 * 유지 시간 동안 위기 프로토콜을 계속 적용한다. 멀티턴에서 위기 컨텍스트가
 * 휘발되는 것을 막는 장치다.
 *
 * 위기 강도는 단계적으로 하강한다 (D-020): 마킹 시 HIGH, 이후 유지 시간마다 한 단계씩
 * MID → LOW → NONE. 새 신호로 다시 mark하면 HIGH로 복귀한다.
 *
 * 현재는 인메모리 구현 — 채팅 REST API에서 세션이 생기면 DB/Redis로 옮긴다.
 */
public class CrisisSessionStore {

    private final Map<String, Instant> markedAt = new ConcurrentHashMap<>();
    private final Duration stickyDuration;
    private final Clock clock;

    public CrisisSessionStore(Duration stickyDuration, Clock clock) {
        this.stickyDuration = stickyDuration;
        this.clock = clock;
    }

    /** 위기 신호 감지 시 호출. 재감지되면 유지 시간이 갱신된다. */
    public void mark(String sessionId) {
        markedAt.put(sessionId, clock.instant());
    }

    /**
     * 세션의 현재 위기 레벨. 마킹 후 경과한 유지 시간 단위 수만큼 HIGH에서 한 단계씩 내려간다.
     * NONE으로 내려가면 세션 항목을 정리한다.
     */
    public CrisisLevel level(String sessionId) {
        Instant marked = markedAt.get(sessionId);
        if (marked == null) {
            return CrisisLevel.NONE;
        }
        long steps = Duration.between(marked, clock.instant()).dividedBy(stickyDuration);
        int rank = CrisisLevel.HIGH.rank() - (int) Math.min(steps, CrisisLevel.HIGH.rank());
        CrisisLevel level = CrisisLevel.ofRank(rank);
        if (level == CrisisLevel.NONE) {
            markedAt.remove(sessionId, marked);
        }
        return level;
    }
}
