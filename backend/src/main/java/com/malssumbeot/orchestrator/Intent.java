package com.malssumbeot.orchestrator;

import java.util.Optional;

/**
 * 사용자 메시지 의도 6분류. label은 분류 프롬프트의 출력 형식과 일치해야 한다.
 */
public enum Intent {

    /** 자살·자해·학대 등 위기 신호 — 모든 분기에 우선한다 (D-004) */
    CRISIS("위기"),
    /** 삶의 고민·감정적 어려움·신앙적 갈등 */
    COUNSELING("상담"),
    /** 기도문 작성 요청 */
    PRAYER("기도문"),
    /** 성경·교리·기독교 역사 지식 질문 */
    KNOWLEDGE_QA("지식QA"),
    /** 인사·근황 등 가벼운 일상 대화 (D-008) */
    DAILY_CHAT("일상대화"),
    /** 코딩·숙제 등 서비스 범위 밖 요청 (D-008) */
    OUT_OF_SCOPE("범위밖");

    private final String label;

    Intent(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Optional<Intent> fromLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }
        String normalized = label.strip();
        for (Intent intent : values()) {
            if (intent.label.equals(normalized)) {
                return Optional.of(intent);
            }
        }
        return Optional.empty();
    }
}
