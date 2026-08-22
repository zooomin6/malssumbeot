package com.malssumbeot.archive;

/** 보관함에 저장하는 구절 스냅샷. 저장 시점에 이미 검증된 원문이라(D-025) 다시 조회하지 않는다. */
public record VerseSnapshot(String reference, String text) {
}
