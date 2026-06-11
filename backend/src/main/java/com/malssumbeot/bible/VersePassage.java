package com.malssumbeot.bible;

import java.util.List;

/**
 * DB에서 검증·조회된 성경 본문. 사용자에게 보내는 구절 본문은 반드시 이 객체를 거친다.
 */
public record VersePassage(String reference, String bookName, int chapter,
                           int verseStart, int verseEnd, List<VerseLine> verses) {

    public record VerseLine(int verse, String text) {
    }

    /** 인용 블록 렌더링용 전체 본문 (절 번호 포함). */
    public String fullText() {
        StringBuilder sb = new StringBuilder();
        for (VerseLine line : verses) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append(line.verse()).append(' ').append(line.text());
        }
        return sb.toString();
    }
}
