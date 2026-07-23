package com.malssumbeot.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IntentTest {

    @Test
    void 여섯_범주_라벨을_전부_해석한다() {
        assertThat(Intent.fromLabel("위기")).contains(Intent.CRISIS);
        assertThat(Intent.fromLabel("상담")).contains(Intent.COUNSELING);
        assertThat(Intent.fromLabel("기도문")).contains(Intent.PRAYER);
        assertThat(Intent.fromLabel("지식QA")).contains(Intent.KNOWLEDGE_QA);
        assertThat(Intent.fromLabel("일상대화")).contains(Intent.DAILY_CHAT);
        assertThat(Intent.fromLabel("범위밖")).contains(Intent.OUT_OF_SCOPE);
    }

    @Test
    void 공백이_섞인_라벨을_허용한다() {
        assertThat(Intent.fromLabel(" 위기\n")).contains(Intent.CRISIS);
    }

    @Test
    void 알_수_없는_라벨은_빈_값을_돌려준다() {
        assertThat(Intent.fromLabel("모름")).isEmpty();
        assertThat(Intent.fromLabel("")).isEmpty();
        assertThat(Intent.fromLabel(null)).isEmpty();
    }

    @Test
    void 상담_기도문_지식QA만_신앙_근거가_필요하다() {
        assertThat(Intent.COUNSELING.requiresGrounding()).isTrue();
        assertThat(Intent.PRAYER.requiresGrounding()).isTrue();
        assertThat(Intent.KNOWLEDGE_QA.requiresGrounding()).isTrue();
        assertThat(Intent.CRISIS.requiresGrounding()).isFalse();
        assertThat(Intent.DAILY_CHAT.requiresGrounding()).isFalse();
        assertThat(Intent.OUT_OF_SCOPE.requiresGrounding()).isFalse();
    }
}
