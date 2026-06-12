package com.malssumbeot.crisis;

/** 감지된 위기 신호. category는 패턴 파일의 분류(자살자해직접/자살자해간접/학대). */
public record CrisisSignal(String category, String matchedPattern) {
}
