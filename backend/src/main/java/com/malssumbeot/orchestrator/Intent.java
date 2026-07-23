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

    /**
     * 신앙 근거(성경 구절)가 필요한 인텐트인지 — 옵션B 그라운딩 파이프라인(1단계 주소 제안) 대상 여부
     * (D-025). ModelRouter의 faithModel 라우팅 대상 중 CRISIS는 위기 분기로 빠져 generate()에
     * 도달하지 않으므로 제외한다.
     */
    public boolean requiresGrounding() {
        return this == COUNSELING || this == PRAYER || this == KNOWLEDGE_QA;
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
