package com.malssumbeot.bible;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 구절 주소 문자열을 해석한다.
 * 지원 형식: "요한복음 3:16", "요 3:16", "빌립보서 4:6-7", "눅 15장 11-32절"
 */
public class VerseReferenceParser {

    private static final Pattern PATTERN = Pattern.compile(
            "^\\s*(?<book>\\D+?)\\s*(?<chapter>\\d{1,3})\\s*(?::|장)\\s*"
                    + "(?<start>\\d{1,3})(?:\\s*[-~]\\s*(?<end>\\d{1,3}))?\\s*절?\\s*$");
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "^\\s*(?<book>\\D+?)\\s*(?<chapter>\\d{1,3})\\s*(?:장|편)\\s*$");

    private final BibleBookCatalog catalog;

    public VerseReferenceParser(BibleBookCatalog catalog) {
        this.catalog = catalog;
    }

    public VerseReference parse(String rawReference) {
        if (rawReference == null || rawReference.isBlank()) {
            throw new InvalidVerseReferenceException("구절 주소가 비어 있습니다.");
        }
        Matcher matcher = PATTERN.matcher(rawReference);
        if (!matcher.matches()) {
            throw new InvalidVerseReferenceException(
                    "구절 주소 형식을 해석할 수 없습니다: " + rawReference);
        }

        BibleBook book = catalog.resolve(matcher.group("book"))
                .orElseThrow(() -> new InvalidVerseReferenceException(
                        "알 수 없는 성경 책 이름입니다: " + matcher.group("book").trim()));

        int chapter = Integer.parseInt(matcher.group("chapter"));
        int verseStart = Integer.parseInt(matcher.group("start"));
        String endGroup = matcher.group("end");
        int verseEnd = endGroup != null ? Integer.parseInt(endGroup) : verseStart;

        if (chapter < 1 || verseStart < 1) {
            throw new InvalidVerseReferenceException(
                    "장과 절은 1 이상이어야 합니다: " + rawReference);
        }
        if (verseEnd < verseStart) {
            throw new InvalidVerseReferenceException(
                    "절 범위가 잘못되었습니다: " + rawReference);
        }

        return new VerseReference(book, chapter, verseStart, verseEnd);
    }

    /** "시편 23편", "눅 15장"처럼 절을 지정하지 않은 장 인용을 해석한다. */
    public ChapterReference parseChapter(String rawReference) {
        if (rawReference == null || rawReference.isBlank()) {
            throw new InvalidVerseReferenceException("성경 장 주소가 비어 있습니다.");
        }
        Matcher matcher = CHAPTER_PATTERN.matcher(rawReference);
        if (!matcher.matches()) {
            throw new InvalidVerseReferenceException(
                    "성경 장 주소 형식을 해석할 수 없습니다: " + rawReference);
        }

        BibleBook book = catalog.resolve(matcher.group("book"))
                .orElseThrow(() -> new InvalidVerseReferenceException(
                        "알 수 없는 성경 책 이름입니다: " + matcher.group("book").trim()));
        int chapter = Integer.parseInt(matcher.group("chapter"));
        if (chapter < 1) {
            throw new InvalidVerseReferenceException(
                    "장은 1 이상이어야 합니다: " + rawReference);
        }
        return new ChapterReference(book, chapter);
    }
}
