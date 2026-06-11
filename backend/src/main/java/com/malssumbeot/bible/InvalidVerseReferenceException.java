package com.malssumbeot.bible;

/** 구절 주소 형식이 잘못되었거나 알 수 없는 책 이름인 경우. */
public class InvalidVerseReferenceException extends RuntimeException {

    public InvalidVerseReferenceException(String message) {
        super(message);
    }
}
