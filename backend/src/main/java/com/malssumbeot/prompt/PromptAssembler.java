package com.malssumbeot.prompt;

import com.malssumbeot.bible.VersePassage;
import com.malssumbeot.orchestrator.Intent;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 시스템 프롬프트 조립: 마스터(공통 베이스) + 모드 분기 + 검증된 성경 원문(<bible_verses>).
 *
 * <bible_verses> 블록에는 BibleVerseService가 DB에서 조회한 원문만 들어간다 (D-003).
 */
@Component
public class PromptAssembler {

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
        }
        return sb.toString();
    }
}
