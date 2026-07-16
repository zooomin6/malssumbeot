package com.malssumbeot.crisis;

/**
 * 위기 sticky 강도 (D-020).
 *
 * 위기 신호 감지 시 HIGH로 설정되고, 이후 새 신호 없이 유지 시간(sticky-duration)이
 * 지날 때마다 한 단계씩 낮아진다: HIGH → MID → LOW → NONE. 대화를 이어가는 동안
 * 급격히 끊기지 않고 서서히 내려가도록 하기 위함이다.
 *
 * 시간 기반으로만 내려간다 — 사용자가 "괜찮아요"라고 말한다고 레벨을 낮추지 않는다
 * (실제 위기를 놓칠 수 있어서). 새 위기 신호가 오면 즉시 HIGH로 복귀한다(안전 우선).
 */
public enum CrisisLevel {

    NONE(0),
    LOW(1),
    MID(2),
    HIGH(3);

    private final int rank;

    CrisisLevel(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    /** 신호 감지 후 낮아진 단계 수만큼 뺀 랭크를 레벨로 변환한다. 0 이하는 NONE. */
    static CrisisLevel ofRank(int rank) {
        if (rank >= HIGH.rank) {
            return HIGH;
        }
        if (rank == MID.rank) {
            return MID;
        }
        if (rank == LOW.rank) {
            return LOW;
        }
        return NONE;
    }
}
