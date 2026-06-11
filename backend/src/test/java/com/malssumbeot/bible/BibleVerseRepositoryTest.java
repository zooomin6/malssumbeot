package com.malssumbeot.bible;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class BibleVerseRepositoryTest {

    @Autowired
    private BibleVerseRepository verseRepository;

    @Test
    void 범위_조회는_절_순서대로_돌려준다() {
        verseRepository.saveAll(List.of(
                new BibleVerse(50, 4, 7, "(테스트 본문 7절)"),
                new BibleVerse(50, 4, 6, "(테스트 본문 6절)"),
                new BibleVerse(50, 4, 5, "(테스트 본문 5절)"),
                new BibleVerse(50, 3, 6, "(다른 장)"),
                new BibleVerse(43, 4, 6, "(다른 책)")));

        List<BibleVerse> rows = verseRepository
                .findByBookIdAndChapterAndVerseBetweenOrderByVerseAsc(50, 4, 6, 7);

        assertThat(rows).extracting(BibleVerse::getVerse).containsExactly(6, 7);
        assertThat(rows).extracting(BibleVerse::getBookId).containsOnly(50);
    }
}
