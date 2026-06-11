package com.malssumbeot.bible.importer;

import com.malssumbeot.bible.BibleBook;
import com.malssumbeot.bible.BibleBookCatalog;
import com.malssumbeot.bible.BibleVerse;
import com.malssumbeot.bible.BibleVerseRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 개역한글 본문 TSV 적재기. bible-import 프로파일에서만 활성화된다.
 *
 * 입력 형식 (UTF-8, 한 줄 = 한 절): 책약어<TAB>장<TAB>절<TAB>본문
 * 예: 요	3	16	...
 *
 * 주의: 적재할 텍스트 파일은 반드시 출처가 검증된 개역한글(1961)이어야 한다 (D-001).
 * 소스 확정은 PROGRESS.md "사람 확인 필요" 항목.
 */
@Component
@Profile("bible-import")
public class BibleTextImporter implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BibleTextImporter.class);
    private static final int BATCH_SIZE = 500;

    private final BibleBookCatalog catalog;
    private final BibleVerseRepository verseRepository;
    private final String importFile;

    public BibleTextImporter(BibleBookCatalog catalog, BibleVerseRepository verseRepository,
                             @Value("${malssumbeot.bible.import-file}") String importFile) {
        this.catalog = catalog;
        this.verseRepository = verseRepository;
        this.importFile = importFile;
    }

    @Override
    public void run(String... args) throws IOException {
        if (importFile == null || importFile.isBlank()) {
            throw new IllegalStateException(
                    "BIBLE_IMPORT_FILE 환경변수(또는 malssumbeot.bible.import-file)를 지정하세요.");
        }
        Path path = Path.of(importFile);
        if (!Files.isReadable(path)) {
            throw new IllegalStateException("임포트 파일을 읽을 수 없습니다: " + path);
        }

        long existing = verseRepository.count();
        if (existing > 0) {
            throw new IllegalStateException(
                    "bible_verse에 이미 %d개의 절이 있습니다. 중복 적재를 막기 위해 중단합니다.".formatted(existing));
        }

        List<BibleVerse> batch = new ArrayList<>(BATCH_SIZE);
        long total = 0;
        int lineNo = 0;
        try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
            for (String line : (Iterable<String>) lines::iterator) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                batch.add(parseLine(line, lineNo));
                if (batch.size() >= BATCH_SIZE) {
                    verseRepository.saveAll(batch);
                    total += batch.size();
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            verseRepository.saveAll(batch);
            total += batch.size();
        }
        log.info("성경 본문 적재 완료: {}절", total);
    }

    BibleVerse parseLine(String line, int lineNo) {
        String[] cols = line.split("\t", -1);
        if (cols.length != 4) {
            throw new IllegalArgumentException(
                    "%d번째 줄: 컬럼이 4개가 아닙니다 (책약어<TAB>장<TAB>절<TAB>본문)".formatted(lineNo));
        }
        BibleBook book = catalog.resolve(cols[0]).orElseThrow(() ->
                new IllegalArgumentException("%d번째 줄: 알 수 없는 책 약어 '%s'".formatted(lineNo, cols[0])));
        int chapter = Integer.parseInt(cols[1].trim());
        int verse = Integer.parseInt(cols[2].trim());
        String text = cols[3].trim();

        if (chapter < 1 || chapter > book.getChapterCount()) {
            throw new IllegalArgumentException(
                    "%d번째 줄: %s은(는) %d장까지 있습니다 (입력: %d장)".formatted(
                            lineNo, book.getNameKo(), book.getChapterCount(), chapter));
        }
        if (verse < 1) {
            throw new IllegalArgumentException("%d번째 줄: 절 번호가 잘못되었습니다".formatted(lineNo));
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("%d번째 줄: 본문이 비어 있습니다".formatted(lineNo));
        }
        return new BibleVerse(book.getId(), chapter, verse, text);
    }
}
