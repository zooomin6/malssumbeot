package com.malssumbeot.crisis;

import java.util.Optional;

/**
 * 위기 판정 결과. crisis=true면 호출부는 다른 모든 로직을 건너뛰고
 * 위기 프로토콜로 분기해야 한다 (절대 원칙 5 — 우회 금지).
 */
public record CrisisCheck(boolean crisis, Trigger trigger, Optional<CrisisSignal> signal) {

    public enum Trigger {
        /** 이번 메시지에서 위기 신호 감지 */
        NEW_SIGNAL,
        /** 직전 메시지가 위기였고, 이번 메시지 1회에 한해 sticky 적용 (D-026) */
        STICKY,
        /** 위기 아님 */
        NONE
    }

    /** 새 신호는 항상 위기로 판정한다. */
    public static CrisisCheck newSignal(CrisisSignal signal) {
        return new CrisisCheck(true, Trigger.NEW_SIGNAL, Optional.of(signal));
    }

    /** sticky 1회 적용 중(D-026). category는 마킹 당시 저장된 값(2차 방어선 판정은 null). */
    public static CrisisCheck sticky(String category) {
        Optional<CrisisSignal> signal = category == null
                ? Optional.empty()
                : Optional.of(new CrisisSignal(category, null));
        return new CrisisCheck(true, Trigger.STICKY, signal);
    }

    public static CrisisCheck none() {
        return new CrisisCheck(false, Trigger.NONE, Optional.empty());
    }
}
