package com.malssumbeot.bible.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

class BibleTextScraperTest {

    @Test
    void 각주와_소제목을_제외하고_절만_추출한다() throws IOException {
        Document doc = loadFixture("bible-scrape/psalm3.html");

        List<BibleTextScraper.Verse> verses = BibleTextScraper.parseVerses(doc);

        assertThat(verses).hasSize(8);
        assertThat(verses.get(0).number()).isEqualTo(1);
        assertThat(verses.get(0).text()).isEqualTo("여호와여 나의 대적이 어찌 그리 많은지요 일어나 나를 치는 자가 많소이다");
    }

    @Test
    void 각주_마커와_각주_내용은_본문에서_제거된다() throws IOException {
        Document doc = loadFixture("bible-scrape/psalm3.html");

        List<BibleTextScraper.Verse> verses = BibleTextScraper.parseVerses(doc);

        String verse2 = verses.get(1).text();
        assertThat(verse2).isEqualTo("많은 사람이 있어 나를 가리켜 말하기를 저는 하나님께 도움을 얻지 못한다 하나이다(셀라)");
        assertThat(verse2).doesNotContain("1)").doesNotContain("구원");
    }

    @Test
    void 장_소제목은_절로_포함되지_않는다() throws IOException {
        Document doc = loadFixture("bible-scrape/psalm3.html");

        List<BibleTextScraper.Verse> verses = BibleTextScraper.parseVerses(doc);

        assertThat(verses).noneMatch(v -> v.text().contains("압살롬"));
        assertThat(verses).noneMatch(v -> v.text().contains("제 3 편"));
    }

    @Test
    void 합쳐진_절_표기는_각_절_번호에_같은_본문을_넣는다() throws IOException {
        Document doc = loadFixture("bible-scrape/merged-verse.html");

        List<BibleTextScraper.Verse> verses = BibleTextScraper.parseVerses(doc);

        assertThat(verses).extracting(BibleTextScraper.Verse::number)
                .containsExactly(17, 18, 19, 20);
        assertThat(verses.get(1).text()).isEqualTo("열여덟과 열아홉째 절이 합쳐진 본문");
        assertThat(verses.get(2).text()).isEqualTo(verses.get(1).text());
    }

    private Document loadFixture(String resourcePath) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("테스트 리소스를 찾을 수 없습니다: " + resourcePath);
            }
            return Jsoup.parse(in, StandardCharsets.UTF_8.name(), "");
        }
    }
}
