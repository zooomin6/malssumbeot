package com.malssumbeot.api;

import com.malssumbeot.bible.VersePassage;
import com.malssumbeot.orchestrator.ChatReply;
import java.util.List;

/**
 * 채팅 API 응답 DTO. 도메인 객체(ChatReply)를 API 계약으로 노출한다.
 *
 * 성경 본문은 DB 검증을 거친 passages에만 담긴다 (D-003) — 모델이 만든 구절 본문은 여기 오지 않는다.
 * passages는 인용 블록을 프로즈와 구분해 렌더링하기 위한 구조화 데이터다.
 */
public record ChatResponse(String text, String intent, boolean crisis,
                           List<Passage> passages, List<String> unverifiedReferences) {

    public record Passage(String reference, String bookName, int chapter,
                          int verseStart, int verseEnd, List<Verse> verses) {

        public record Verse(int verse, String text) {
        }
    }

    public static ChatResponse from(ChatReply reply) {
        List<Passage> passages = reply.passages().stream()
                .map(ChatResponse::toPassage)
                .toList();
        return new ChatResponse(reply.text(), reply.intent().name(), reply.crisis(),
                passages, reply.unverifiedReferences());
    }

    private static Passage toPassage(VersePassage p) {
        List<Passage.Verse> verses = p.verses().stream()
                .map(v -> new Passage.Verse(v.verse(), v.text()))
                .toList();
        return new Passage(p.reference(), p.bookName(), p.chapter(),
                p.verseStart(), p.verseEnd(), verses);
    }
}
