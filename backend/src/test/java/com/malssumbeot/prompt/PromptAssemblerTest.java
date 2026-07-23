package com.malssumbeot.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.malssumbeot.bible.VersePassage;
import com.malssumbeot.orchestrator.Intent;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptAssemblerTest {

    private final PromptAssembler assembler = new PromptAssembler(new PromptRepository());

    @Test
    void 마스터와_모드_프롬프트를_조립한다() {
        String prompt = assembler.assemble(Intent.COUNSELING, List.of());

        assertThat(prompt).contains("말씀벗");                  // 마스터
        assertThat(prompt).contains("<mode>상담</mode>");       // 분기
        // 구절이 없으면 블록 생략 — 마스터 본문에 "<bible_verses>" 언급이 있어 닫는 태그로 검증
        assertThat(prompt).doesNotContain("</bible_verses>");
    }

    @Test
    void 모든_의도에_대응_프롬프트가_있다() {
        for (Intent intent : Intent.values()) {
            assertThat(assembler.assemble(intent, List.of()))
                    .as("intent=%s", intent)
                    .contains("말씀벗");
        }
    }

    @Test
    void 위기_프롬프트는_전문기관_연락처를_담는다() {
        String prompt = assembler.assemble(Intent.CRISIS, List.of());

        assertThat(prompt).contains("최우선");
        assertThat(prompt).contains("109");
        assertThat(prompt).contains("1577-0199");
    }

    @Test
    void 검증된_구절은_bible_verses_블록으로_들어간다() {
        VersePassage passage = new VersePassage("빌립보서 4:6-7", "빌립보서", 4, 6, 7,
                List.of(new VersePassage.VerseLine(6, "(테스트 본문)")));

        String prompt = assembler.assemble(Intent.COUNSELING, List.of(passage));

        assertThat(prompt).contains("<bible_verses>");
        assertThat(prompt).contains("[빌립보서 4:6-7]");
        assertThat(prompt).contains("6 (테스트 본문)");
    }

    @Test
    void 신앙_근거가_필요한_인텐트인데_검증된_구절이_없으면_인용_금지_안내를_붙인다() {
        for (Intent intent : List.of(Intent.COUNSELING, Intent.PRAYER, Intent.KNOWLEDGE_QA)) {
            String prompt = assembler.assemble(intent, List.of());

            assertThat(prompt).as("intent=%s", intent).contains("단정적 표현을 쓰지 마세요");
        }
    }

    @Test
    void 신앙_근거가_필요없는_인텐트는_검증된_구절이_없어도_안내를_붙이지_않는다() {
        for (Intent intent : List.of(Intent.DAILY_CHAT, Intent.OUT_OF_SCOPE, Intent.CRISIS)) {
            String prompt = assembler.assemble(intent, List.of());

            assertThat(prompt).as("intent=%s", intent).doesNotContain("단정적 표현을 쓰지 마세요");
        }
    }
}
