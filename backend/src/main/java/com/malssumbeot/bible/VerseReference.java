package com.malssumbeot.bible;

/**
 * 해석이 끝난 구절 주소. verseStart == verseEnd 이면 단일 절.
 */
public record VerseReference(BibleBook book, int chapter, int verseStart, int verseEnd) {

    public int verseCount() {
        return verseEnd - verseStart + 1;
    }

    public String display() {
        if (verseStart == verseEnd) {
            return "%s %d:%d".formatted(book.getNameKo(), chapter, verseStart);
        }
        return "%s %d:%d-%d".formatted(book.getNameKo(), chapter, verseStart, verseEnd);
    }
}
