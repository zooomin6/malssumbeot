package com.malssumbeot.prompt;

import com.malssumbeot.bible.VersePassage;
import com.malssumbeot.orchestrator.Intent;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 시스템 프롬프트 조립: 마스터(공통 베이스) + 모드 분기 + 검증된 성경 원문(<bible_verses>).
 *
 * <bible_verses> 블록에는 BibleVerseService가 DB에서 조회한 원문만 들어간다 (D-003).
 *
 * 신앙 근거가 필요한 인텐트(상담/기도문/지식QA)인데 검증된 구절이 하나도 없으면, 근거 없이
 * 성경을 인용하지 말라는 안내를 대신 붙인다 — 주소 없는 신앙적 단정이 검증을 우회하는 것을
 * 막기 위함이다 (P0-2, D-025).
 */
@Component
public class PromptAssembler {

    private static final String NO_VERIFIED_PASSAGE_NOTICE = """
            [안내] 이번 대화에는 시스템이 확인한 성경 구절이 없습니다. 이 경우 성경 구절을 인용하거나
            "성경은 ~라고 말합니다" 같은 단정적 표현을 쓰지 마세요. 대신 공감과 위로로 답하거나,
            필요하면 출석 교회 목회자와 나눠볼 것을 권하세요.""";

    private final PromptRepository prompts;

    public PromptAssembler(PromptRepository prompts) {
        this.prompts = prompts;
    }

    public String assemble(Intent intent, List<VersePassage> verifiedPassages) {
        StringBuilder sb = new StringBuilder(prompts.master());
        sb.append("\n\n").append(prompts.forIntent(intent));
        if (verifiedPassages != null && !verifiedPassages.isEmpty()) {
            sb.append("\n\n<bible_verses>\n");
            for (VersePassage passage : verifiedPassages) {
                sb.append("[").append(passage.reference()).append("]\n")
                        .append(passage.fullText()).append('\n');
            }
            sb.append("</bible_verses>");
        } else if (intent.requiresGrounding()) {
            sb.append("\n\n").append(NO_VERIFIED_PASSAGE_NOTICE);
        }
        return sb.toString();
    }
}
