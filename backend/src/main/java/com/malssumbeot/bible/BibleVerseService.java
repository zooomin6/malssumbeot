package com.malssumbeot.bible;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구절 주소 → DB 원문 조회 + 존재 검증 (D-003).
 *
 * 모델은 구절 주소만 제안하고, 사용자에게 전달되는 본문은 반드시 이 서비스가
 * 조회한 DB 원문이어야 한다. 모델 출력의 구절 본문을 그대로 내보내는 경로는 금지.
 */
@Service
@Transactional(readOnly = true)
public class BibleVerseService {

    private final VerseReferenceParser parser;
    private final BibleVerseRepository verseRepository;

    public BibleVerseService(VerseReferenceParser parser, BibleVerseRepository verseRepository) {
        this.parser = parser;
        this.verseRepository = verseRepository;
    }

    /**
     * @throws InvalidVerseReferenceException 주소 형식 오류 또는 알 수 없는 책 이름
     * @throws VerseNotFoundException        DB에 존재하지 않는 구절 (환각 의심)
     */
    public VersePassage getPassage(String rawReference) {
        VerseReference ref = parser.parse(rawReference);

        if (ref.chapter() > ref.book().getChapterCount()) {
            throw new VerseNotFoundException(
                    "%s은(는) %d장까지 있습니다: %s".formatted(
                            ref.book().getNameKo(), ref.book().getChapterCount(), ref.display()));
        }

        List<BibleVerse> rows = verseRepository.findByBookIdAndChapterAndVerseBetweenOrderByVerseAsc(
                ref.book().getId(), ref.chapter(), ref.verseStart(), ref.verseEnd());

        if (rows.size() < ref.verseCount()) {
            throw new VerseNotFoundException("성경 DB에 존재하지 않는 구절입니다: " + ref.display());
        }

        List<VersePassage.VerseLine> lines = rows.stream()
                .map(row -> new VersePassage.VerseLine(row.getVerse(), row.getText()))
                .toList();

        return new VersePassage(ref.display(), ref.book().getNameKo(), ref.chapter(),
                ref.verseStart(), ref.verseEnd(), lines);
    }

    /** 예외 대신 Optional이 필요한 호출부용. 환각 검증(존재 여부 확인)에도 사용한다. */
    public Optional<VersePassage> findPassage(String rawReference) {
        try {
            return Optional.of(getPassage(rawReference));
        } catch (InvalidVerseReferenceException | VerseNotFoundException e) {
            return Optional.empty();
        }
    }
}
