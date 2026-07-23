package com.malssumbeot.api;

import com.malssumbeot.bible.VersePassage;
import com.malssumbeot.orchestrator.ChatReply;
import java.util.List;

/**
 * 채팅 API 응답 DTO. 도메인 객체(ChatReply)를 API 계약으로 노출한다.
 *
 * 성경 본문은 DB 검증을 거친 passages에만 담긴다 (D-003) — 모델이 만든 구절 본문은 여기 오지 않는다.
 * passages는 인용 블록을 프로즈와 구분해 렌더링하기 위한 구조화 데이터다.
 *
 * 검증 실패(환각 의심) 구절 주소는 클라이언트에 노출하지 않는다 — 서버 로그에서만 추적한다
 * (ChatOrchestrator의 검증 로그 참고).
 */
public record ChatResponse(String text, String intent, boolean crisis, List<Passage> passages) {

    public record Passage(String reference, String bookName, int chapter,
                          int verseStart, int verseEnd, List<Verse> verses) {

        public record Verse(int verse, String text) {
        }
    }

    public static ChatResponse from(ChatReply reply) {
        List<Passage> passages = reply.passages().stream()
                .map(ChatResponse::toPassage)
                .toList();
        return new ChatResponse(reply.text(), reply.intent().name(), reply.crisis(), passages);
    }

    private static Passage toPassage(VersePassage p) {
        List<Passage.Verse> verses = p.verses().stream()
                .map(v -> new Passage.Verse(v.verse(), v.text()))
                .toList();
        return new Passage(p.reference(), p.bookName(), p.chapter(),
                p.verseStart(), p.verseEnd(), verses);
    }
}
