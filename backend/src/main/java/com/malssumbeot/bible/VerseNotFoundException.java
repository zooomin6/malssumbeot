package com.malssumbeot.bible;

/**
 * 주소 형식은 올바르지만 해당 구절이 성경 DB에 존재하지 않는 경우.
 * 모델이 제안한 구절의 환각 여부를 판정하는 신호로 사용한다 (QA T8).
 */
public class VerseNotFoundException extends RuntimeException {

    public VerseNotFoundException(String message) {
        super(message);
    }
}
