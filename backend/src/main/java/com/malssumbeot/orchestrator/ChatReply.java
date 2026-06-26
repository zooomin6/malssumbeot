package com.malssumbeot.orchestrator;

import com.malssumbeot.bible.VersePassage;
import java.util.List;

/**
 * 오케스트레이터의 최종 응답. text는 모델 생성 텍스트,
 * passages는 BibleVerseService가 DB에서 검증·조회한 원문(UI 인용 블록용)이다.
 * 모델이 생성한 구절 본문은 passages에 절대 들어가지 않는다 (D-003).
 */
public record ChatReply(String text, Intent intent, boolean crisis,
                        List<VersePassage> passages, List<String> unverifiedReferences) {
}
