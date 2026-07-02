package com.malssumbeot.bible.importer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 대한성서공회 공식 성경읽기 페이지(bskorea.or.kr)에서 개역한글(HAN) 본문을 긁어와
 * BibleTextImporter가 요구하는 TSV(책약어<TAB>장<TAB>절<TAB>본문)를 만든다.
 *
 * Spring/DB 불필요 — 독립 실행 도구. 생성된 파일을 BIBLE_IMPORT_FILE로 지정해
 * bible-import 프로파일로 적재한다 (backend/README.md 참조).
 *
 * 절 파싱 근거: 원본 HTML은 절 번호를 <span class="number">, 각주 마커를
 * <a class="comment">, 각주 내용을 <div class="D2" style="display:none">,
 * 장/소제목을 <font class="chapNum">/<font class="smallTitle">로 감싼다
 * (2026-07-02 실제 응답 확인, 시편 3장·창세기 1장·요한복음 3장 대조 검증).
 */
public final class BibleTextScraper {

    private static final String BASE_URL = "https://www.bskorea.or.kr/bible/korbibReadpage.php";
    private static final long REQUEST_DELAY_MS = 2000;
    private static final int MAX_ATTEMPTS = 3;

    /** siteCode: bskorea.or.kr 요청용 코드. dbCode: 우리 bible_book.code (요나만 다름: jnh vs jon). */
    record BookInfo(String siteCode, String dbCode, int chapterCount) {
    }

    static final List<BookInfo> BOOKS = List.of(
            new BookInfo("gen", "gen", 50),
            new BookInfo("exo", "exo", 40),
            new BookInfo("lev", "lev", 27),
            new BookInfo("num", "num", 36),
            new BookInfo("deu", "deu", 34),
            new BookInfo("jos", "jos", 24),
            new BookInfo("jdg", "jdg", 21),
            new BookInfo("rut", "rut", 4),
            new BookInfo("1sa", "1sa", 31),
            new BookInfo("2sa", "2sa", 24),
            new BookInfo("1ki", "1ki", 22),
            new BookInfo("2ki", "2ki", 25),
            new BookInfo("1ch", "1ch", 29),
            new BookInfo("2ch", "2ch", 36),
            new BookInfo("ezr", "ezr", 10),
            new BookInfo("neh", "neh", 13),
            new BookInfo("est", "est", 10),
            new BookInfo("job", "job", 42),
            new BookInfo("psa", "psa", 150),
            new BookInfo("pro", "pro", 31),
            new BookInfo("ecc", "ecc", 12),
            new BookInfo("sng", "sng", 8),
            new BookInfo("isa", "isa", 66),
            new BookInfo("jer", "jer", 52),
            new BookInfo("lam", "lam", 5),
            new BookInfo("ezk", "ezk", 48),
            new BookInfo("dan", "dan", 12),
            new BookInfo("hos", "hos", 14),
            new BookInfo("jol", "jol", 3),
            new BookInfo("amo", "amo", 9),
            new BookInfo("oba", "oba", 1),
            new BookInfo("jnh", "jon", 4),
            new BookInfo("mic", "mic", 7),
            new BookInfo("nam", "nam", 3),
            new BookInfo("hab", "hab", 3),
            new BookInfo("zep", "zep", 3),
            new BookInfo("hag", "hag", 2),
            new BookInfo("zec", "zec", 14),
            new BookInfo("mal", "mal", 4),
            new BookInfo("mat", "mat", 28),
            new BookInfo("mrk", "mrk", 16),
            new BookInfo("luk", "luk", 24),
            new BookInfo("jhn", "jhn", 21),
            new BookInfo("act", "act", 28),
            new BookInfo("rom", "rom", 16),
            new BookInfo("1co", "1co", 16),
            new BookInfo("2co", "2co", 13),
            new BookInfo("gal", "gal", 6),
            new BookInfo("eph", "eph", 6),
            new BookInfo("php", "php", 4),
            new BookInfo("col", "col", 4),
            new BookInfo("1th", "1th", 5),
            new BookInfo("2th", "2th", 3),
            new BookInfo("1ti", "1ti", 6),
            new BookInfo("2ti", "2ti", 4),
            new BookInfo("tit", "tit", 3),
            new BookInfo("phm", "phm", 1),
            new BookInfo("heb", "heb", 13),
            new BookInfo("jas", "jas", 5),
            new BookInfo("1pe", "1pe", 5),
            new BookInfo("2pe", "2pe", 3),
            new BookInfo("1jn", "1jn", 5),
            new BookInfo("2jn", "2jn", 1),
            new BookInfo("3jn", "3jn", 1),
            new BookInfo("jud", "jud", 1),
            new BookInfo("rev", "rev", 22)
    );

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("사용법: BibleTextScraper <출력 TSV 경로>");
        }
        Path output = Path.of(args[0]);
        HttpClient client = HttpClient.newHttpClient();

        long verseCount = 0;
        try (var writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            for (BookInfo book : BOOKS) {
                for (int chap = 1; chap <= book.chapterCount(); chap++) {
                    for (Verse v : fetchVerses(client, book.siteCode(), chap)) {
                        writer.write(book.dbCode() + "\t" + chap + "\t" + v.number() + "\t" + v.text() + "\n");
                        verseCount++;
                    }
                    Thread.sleep(REQUEST_DELAY_MS);
                }
                System.out.println(book.dbCode() + " 완료 (누적 " + verseCount + "절)");
            }
        }
        System.out.println("총 " + verseCount + "절 저장: " + output);
    }

    record Verse(int number, String text) {
    }

    private static List<Verse> fetchVerses(HttpClient client, String siteCode, int chap)
            throws IOException, InterruptedException {
        IOException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Document doc = fetchChapter(client, siteCode, chap);
                List<Verse> verses = parseVerses(doc);
                if (verses.isEmpty()) {
                    throw new IOException(siteCode + " " + chap + "장: 파싱된 절이 0개입니다");
                }
                return verses;
            } catch (IOException e) {
                lastError = e;
                Thread.sleep(REQUEST_DELAY_MS * attempt);
            }
        }
        throw lastError;
    }

    private static Document fetchChapter(HttpClient client, String siteCode, int chap)
            throws IOException, InterruptedException {
        URI uri = URI.create(BASE_URL + "?version=HAN&book=" + siteCode + "&chap=" + chap);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", "Mozilla/5.0 (malssumbeot bible text collector)")
                .GET()
                .build();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException(siteCode + " " + chap + "장 요청 실패: HTTP " + response.statusCode());
        }
        return Jsoup.parse(response.body());
    }

    static List<Verse> parseVerses(Document doc) {
        Element root = doc.selectFirst("div.bible_read");
        if (root == null) {
            throw new IllegalStateException("본문 영역(div.bible_read)을 찾을 수 없습니다");
        }
        root.select("a.comment, div.D2, font.smallTitle, font.chapNum").remove();

        List<Verse> verses = new ArrayList<>();
        for (Element numberSpan : root.select("span.number")) {
            String numberText = numberSpan.text().trim();
            Element verseSpan = numberSpan.parent();
            numberSpan.remove();
            String text = verseSpan.text().trim();
            if (!text.isEmpty()) {
                for (int number : parseVerseNumbers(numberText)) {
                    verses.add(new Verse(number, text));
                }
            }
        }
        return verses;
    }

    /** "18" -> [18]. "18-19"처럼 두 절이 한 문장으로 합쳐진 표기는 각 절 번호 전부에 같은 본문을 매핑한다. */
    static List<Integer> parseVerseNumbers(String numberText) {
        if (numberText.contains("-")) {
            String[] parts = numberText.split("-", 2);
            int start = Integer.parseInt(parts[0].trim());
            int end = Integer.parseInt(parts[1].trim());
            List<Integer> numbers = new ArrayList<>();
            for (int n = start; n <= end; n++) {
                numbers.add(n);
            }
            return numbers;
        }
        return List.of(Integer.parseInt(numberText));
    }

    private BibleTextScraper() {
    }
}
