package com.malssumbeot.bible;

/** 절을 지정하지 않은 성경 장 인용. */
public record ChapterReference(BibleBook book, int chapter) {
}
