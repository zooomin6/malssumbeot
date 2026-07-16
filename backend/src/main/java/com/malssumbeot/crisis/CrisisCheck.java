package com.malssumbeot.crisis;

import java.util.Optional;

/**
 * 위기 판정 결과. crisis=true면 호출부는 다른 모든 로직을 건너뛰고
 * 위기 프로토콜로 분기해야 한다 (절대 원칙 5 — 우회 금지).
 */
public record CrisisCheck(boolean crisis, Trigger trigger, Optional<CrisisSignal> signal,
                          CrisisLevel level) {

    public enum Trigger {
        /** 이번 메시지에서 위기 신호 감지 */
        NEW_SIGNAL,
        /** 이번 메시지는 평범하지만 세션이 위기 상태 유지 중 */
        STICKY,
        /** 위기 아님 */
        NONE
    }

    /** 새 신호는 항상 최고 강도(HIGH)로 판정한다. */
    public static CrisisCheck newSignal(CrisisSignal signal) {
        return new CrisisCheck(true, Trigger.NEW_SIGNAL, Optional.of(signal), CrisisLevel.HIGH);
    }

    /** sticky 유지 중. 강도는 마킹 후 경과 시간에 따라 하강한 값을 전달한다 (D-020). */
    public static CrisisCheck sticky(CrisisLevel level) {
        return new CrisisCheck(true, Trigger.STICKY, Optional.empty(), level);
    }

    public static CrisisCheck none() {
        return new CrisisCheck(false, Trigger.NONE, Optional.empty(), CrisisLevel.NONE);
    }
}
