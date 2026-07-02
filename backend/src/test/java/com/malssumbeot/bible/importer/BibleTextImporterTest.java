package com.malssumbeot.bible.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.malssumbeot.bible.BibleTestFixtures;
import com.malssumbeot.bible.BibleVerse;
import com.malssumbeot.bible.BibleVerseRepository;
import org.junit.jupiter.api.Test;

class BibleTextImporterTest {

    private final BibleTextImporter importer = new BibleTextImporter(
            BibleTestFixtures.catalog(), mock(BibleVerseRepository.class), "unused.tsv");

    @Test
    void TSV_한_줄을_절로_변환한다() {
        BibleVerse verse = importer.parseLine("요\t3\t16\t(테스트 본문)", 1);

        assertThat(verse.getBookId()).isEqualTo(43);
        assertThat(verse.getChapter()).isEqualTo(3);
        assertThat(verse.getVerse()).isEqualTo(16);
        assertThat(verse.getText()).isEqualTo("(테스트 본문)");
    }

    @Test
    void 영문_책_코드로도_해석한다() {
        BibleVerse verse = importer.parseLine("jhn\t3\t16\t(테스트 본문)", 1);

        assertThat(verse.getBookId()).isEqualTo(43);
    }

    @Test
    void 알_수_없는_책_약어는_거부한다() {
        assertThatThrownBy(() -> importer.parseLine("없음\t1\t1\t본문", 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3번째 줄");
    }

    @Test
    void 장_범위를_벗어나면_거부한다() {
        assertThatThrownBy(() -> importer.parseLine("빌\t99\t1\t본문", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4장");
    }

    @Test
    void 컬럼_수가_틀리면_거부한다() {
        assertThatThrownBy(() -> importer.parseLine("요\t3\t16", 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
