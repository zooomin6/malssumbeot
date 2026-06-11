package com.malssumbeot.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class IntentClassifierTest {

    private static final String MODEL = "claude-haiku-4-5";

    private final ClaudeChat claudeChat = mock(ClaudeChat.class);
    private final IntentClassifier classifier = new IntentClassifier(claudeChat, MODEL);

    @Test
    void 모델_라벨_출력을_의도로_변환한다() {
        when(claudeChat.complete(eq(MODEL), anyInt(), anyString(), eq("죽고 싶어")))
                .thenReturn("위기");

        assertThat(classifier.classify("죽고 싶어")).isEqualTo(Intent.CRISIS);
    }

    @Test
    void 라벨_주변_공백을_허용한다() {
        when(claudeChat.complete(eq(MODEL), anyInt(), anyString(), anyString()))
                .thenReturn(" 기도문\n");

        assertThat(classifier.classify("면접 잘 보게 기도해줘")).isEqualTo(Intent.PRAYER);
    }

    @Test
    void 형식을_어긴_출력이라도_위기가_포함되면_위기로_분류한다() {
        when(claudeChat.complete(eq(MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("위기 (자살 암시가 보입니다)");

        assertThat(classifier.classify("그냥 다 사라졌으면 좋겠어")).isEqualTo(Intent.CRISIS);

        when(claudeChat.complete(eq(MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("분류 결과: 위기");

        assertThat(classifier.classify("더는 못 버티겠어")).isEqualTo(Intent.CRISIS);
    }

    @Test
    void 해석_불가_출력은_상담으로_폴백한다() {
        when(claudeChat.complete(eq(MODEL), anyInt(), anyString(), anyString()))
                .thenReturn("이 메시지는 상담으로 보입니다.");

        assertThat(classifier.classify("요즘 너무 힘들어"))
                .isEqualTo(IntentClassifier.FALLBACK_INTENT);
    }

    @Test
    void 빈_메시지는_모델_호출_없이_일상대화로_처리한다() {
        assertThat(classifier.classify("  ")).isEqualTo(Intent.DAILY_CHAT);
        assertThat(classifier.classify(null)).isEqualTo(Intent.DAILY_CHAT);
        verifyNoInteractions(claudeChat);
    }

    @Test
    void 분류_프롬프트와_사용자_메시지를_모델에_전달한다() {
        when(claudeChat.complete(anyString(), anyInt(), anyString(), anyString()))
                .thenReturn("지식QA");

        classifier.classify("방언이 뭐야?");

        verify(claudeChat).complete(eq(MODEL), anyInt(), contains("의도 분류기"), eq("방언이 뭐야?"));
    }
}
