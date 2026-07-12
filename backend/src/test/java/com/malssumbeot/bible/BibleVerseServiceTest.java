package com.malssumbeot.bible;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class BibleVerseServiceTest {

    private final BibleVerseRepository verseRepository = mock(BibleVerseRepository.class);
    private final BibleVerseService service = new BibleVerseService(
            new VerseReferenceParser(BibleTestFixtures.catalog()), verseRepository);

    @Test
    void DB_원문으로_본문을_조립한다() {
        when(verseRepository.findByBookIdAndChapterAndVerseBetweenOrderByVerseAsc(50, 4, 6, 7))
                .thenReturn(List.of(
                        new BibleVerse(50, 4, 6, "(테스트 본문 6절)"),
                        new BibleVerse(50, 4, 7, "(테스트 본문 7절)")));

        VersePassage passage = service.getPassage("빌 4:6-7");

        assertThat(passage.reference()).isEqualTo("빌립보서 4:6-7");
        assertThat(passage.bookName()).isEqualTo("빌립보서");
        assertThat(passage.verses()).containsExactly(
                new VersePassage.VerseLine(6, "(테스트 본문 6절)"),
                new VersePassage.VerseLine(7, "(테스트 본문 7절)"));
        assertThat(passage.fullText()).isEqualTo("6 (테스트 본문 6절)\n7 (테스트 본문 7절)");
    }

    @Test
    void DB에_없는_구절은_거부한다() {
        when(verseRepository.findByBookIdAndChapterAndVerseBetweenOrderByVerseAsc(
                anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getPassage("요 3:16"))
                .isInstanceOf(VerseNotFoundException.class)
                .hasMessageContaining("요한복음 3:16");
    }

    @Test
    void 범위_중_일부만_존재하면_거부한다() {
        when(verseRepository.findByBookIdAndChapterAndVerseBetweenOrderByVerseAsc(50, 4, 6, 8))
                .thenReturn(List.of(
                        new BibleVerse(50, 4, 6, "(테스트 본문)"),
                        new BibleVerse(50, 4, 7, "(테스트 본문)")));

        assertThatThrownBy(() -> service.getPassage("빌 4:6-8"))
                .isInstanceOf(VerseNotFoundException.class);
    }

    @Test
    void 존재하지_않는_장은_DB_조회_전에_거부한다() {
        assertThatThrownBy(() -> service.getPassage("빌립보서 99:1"))
                .isInstanceOf(VerseNotFoundException.class)
                .hasMessageContaining("4장");
    }

    @Test
    void findPassage는_환각_구절에_빈_값을_돌려준다() {
        when(verseRepository.findByBookIdAndChapterAndVerseBetweenOrderByVerseAsc(
                anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        assertThat(service.findPassage("요 3:16")).isEmpty();
        assertThat(service.findPassage("도마복음 1:1")).isEmpty();
        assertThat(service.findPassage("이상한 입력")).isEmpty();
    }

    @Test
    void 장_단위_인용의_존재를_책_메타데이터로_검증한다() {
        assertThat(service.hasChapter("시편 23편")).isTrue();
        assertThat(service.hasChapter("빌립보서 99장")).isFalse();
        assertThat(service.hasChapter("도마복음 1장")).isFalse();
    }
}
