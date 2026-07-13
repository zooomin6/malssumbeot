package com.malssumbeot.bible;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VerseReferenceParserTest {

    private final VerseReferenceParser parser = new VerseReferenceParser(BibleTestFixtures.catalog());

    @Test
    void 풀네임_단일_절을_해석한다() {
        VerseReference ref = parser.parse("요한복음 3:16");

        assertThat(ref.book().getId()).isEqualTo(43);
        assertThat(ref.chapter()).isEqualTo(3);
        assertThat(ref.verseStart()).isEqualTo(16);
        assertThat(ref.verseEnd()).isEqualTo(16);
        assertThat(ref.display()).isEqualTo("요한복음 3:16");
    }

    @Test
    void 약어를_해석한다() {
        VerseReference ref = parser.parse("요 3:16");

        assertThat(ref.book().getNameKo()).isEqualTo("요한복음");
    }

    @Test
    void 절_범위를_해석한다() {
        VerseReference ref = parser.parse("빌립보서 4:6-7");

        assertThat(ref.verseStart()).isEqualTo(6);
        assertThat(ref.verseEnd()).isEqualTo(7);
        assertThat(ref.verseCount()).isEqualTo(2);
        assertThat(ref.display()).isEqualTo("빌립보서 4:6-7");
    }

    @Test
    void 장절_표기를_해석한다() {
        VerseReference ref = parser.parse("눅 15장 11-32절");

        assertThat(ref.book().getNameKo()).isEqualTo("누가복음");
        assertThat(ref.chapter()).isEqualTo(15);
        assertThat(ref.verseStart()).isEqualTo(11);
        assertThat(ref.verseEnd()).isEqualTo(32);
    }

    @Test
    void 장_단위_인용을_해석한다() {
        ChapterReference psalm = parser.parseChapter("시편 23편");
        ChapterReference luke = parser.parseChapter("눅 15장");

        assertThat(psalm.book()).isEqualTo(BibleTestFixtures.PSALMS);
        assertThat(psalm.chapter()).isEqualTo(23);
        assertThat(luke.book()).isEqualTo(BibleTestFixtures.LUKE);
        assertThat(luke.chapter()).isEqualTo(15);
    }

    @Test
    void 공백이_섞인_입력을_허용한다() {
        VerseReference ref = parser.parse("  시편  23 : 1  ");

        assertThat(ref.book().getNameKo()).isEqualTo("시편");
        assertThat(ref.chapter()).isEqualTo(23);
    }

    @Test
    void 알_수_없는_책_이름은_거부한다() {
        assertThatThrownBy(() -> parser.parse("도마복음 1:1"))
                .isInstanceOf(InvalidVerseReferenceException.class)
                .hasMessageContaining("도마복음");
    }

    @Test
    void 형식이_잘못된_주소는_거부한다() {
        assertThatThrownBy(() -> parser.parse("요한복음"))
                .isInstanceOf(InvalidVerseReferenceException.class);
        assertThatThrownBy(() -> parser.parse("그냥 문장입니다"))
                .isInstanceOf(InvalidVerseReferenceException.class);
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(InvalidVerseReferenceException.class);
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(InvalidVerseReferenceException.class);
    }

    @Test
    void 역순_절_범위는_거부한다() {
        assertThatThrownBy(() -> parser.parse("요 3:16-2"))
                .isInstanceOf(InvalidVerseReferenceException.class);
    }

    @Test
    void 영_장은_거부한다() {
        assertThatThrownBy(() -> parser.parse("요 0:1"))
                .isInstanceOf(InvalidVerseReferenceException.class);
    }
}
