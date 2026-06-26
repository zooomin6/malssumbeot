package com.malssumbeot.bible;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VerseReferenceScannerTest {

    private final VerseReferenceScanner scanner =
            new VerseReferenceScanner(new VerseReferenceParser(BibleTestFixtures.catalog()));

    @Test
    void 텍스트에서_구절_주소를_찾는다() {
        String text = "이런 말씀이 떠오르네요. 빌립보서 4:6-7을 읽어보시면 어떨까요. 요 3:16도 함께요.";

        assertThat(scanner.scan(text)).containsExactly("빌립보서 4:6-7", "요 3:16");
    }

    @Test
    void 시각_표현은_구절로_오인하지_않는다() {
        assertThat(scanner.scan("내일 오후 3:30에 만나요")).isEmpty();
        assertThat(scanner.scan("점수는 12:7이었어요")).isEmpty();
    }

    @Test
    void 같은_구절은_한_번만_돌려준다() {
        String text = "시편 23:1, 그리고 다시 시편 23:1";

        assertThat(scanner.scan(text)).containsExactly("시편 23:1");
    }

    @Test
    void 빈_텍스트는_빈_목록() {
        assertThat(scanner.scan("")).isEmpty();
        assertThat(scanner.scan(null)).isEmpty();
        assertThat(scanner.scan("구절이 없는 평범한 위로의 말")).isEmpty();
    }
}
