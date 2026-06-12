package com.malssumbeot.crisis;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 위기 감지 필터 — 파이프라인 최앞단 (의도 분류보다 먼저, D-004).
 *
 * 채팅 REST API가 생기면 Spring 인터셉터/필터 레벨에 배선해
 * 어떤 핸들러도 이 검사를 건너뛸 수 없게 한다 (CLAUDE.md 컨벤션).
 */
public class CrisisFilter {

    private static final Logger log = LoggerFactory.getLogger(CrisisFilter.class);

    private final CrisisDetector detector;
    private final CrisisSessionStore sessionStore;

    public CrisisFilter(CrisisDetector detector, CrisisSessionStore sessionStore) {
        this.detector = detector;
        this.sessionStore = sessionStore;
    }

    public CrisisCheck check(String sessionId, String message) {
        Optional<CrisisSignal> signal = detector.detect(message);
        if (signal.isPresent()) {
            sessionStore.mark(sessionId);
            // 메시지 본문은 로그에 남기지 않는다 (민감 정보)
            log.info("위기 신호 감지: session={}, category={}", sessionId, signal.get().category());
            return CrisisCheck.newSignal(signal.get());
        }
        if (sessionStore.isActive(sessionId)) {
            log.info("위기 상태 유지 중(sticky): session={}", sessionId);
            return CrisisCheck.sticky();
        }
        return CrisisCheck.none();
    }
}
