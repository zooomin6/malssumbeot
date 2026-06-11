package com.malssumbeot.bible;

import java.util.List;

/**
 * 테스트용 책 메타데이터. 본문 텍스트는 개역한글 소스 확정 전이므로
 * 실제 성경 본문이 아닌 플레이스홀더만 사용한다 (모델 기억 인용 금지 원칙).
 */
public final class BibleTestFixtures {

    public static final BibleBook PSALMS = new BibleBook(19, "psa", "시편", "시", Testament.OT, 150);
    public static final BibleBook LUKE = new BibleBook(42, "luk", "누가복음", "눅", Testament.NT, 24);
    public static final BibleBook JOHN = new BibleBook(43, "jhn", "요한복음", "요", Testament.NT, 21);
    public static final BibleBook PHILIPPIANS = new BibleBook(50, "php", "빌립보서", "빌", Testament.NT, 4);

    public static BibleBookCatalog catalog() {
        return new BibleBookCatalog(List.of(PSALMS, LUKE, JOHN, PHILIPPIANS));
    }

    private BibleTestFixtures() {
    }
}
