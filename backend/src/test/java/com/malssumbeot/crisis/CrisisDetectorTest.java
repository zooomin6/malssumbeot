package com.malssumbeot.crisis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class CrisisDetectorTest {

    private final CrisisDetector detector = CrisisTestSupport.productionDetector();

    @Test
    void 직접_표현을_감지한다() {
        assertThat(detector.detect("죽고 싶어")).isPresent();
        assertThat(detector.detect("그냥 자살하고 싶다는 생각이 들어")).isPresent();
        assertThat(detector.detect("더 이상 살고 싶지 않아요")).isPresent();
    }

    @Test
    void 간접_표현을_감지한다() {
        assertThat(detector.detect("그냥 다 사라졌으면 좋겠어")).isPresent();
        assertThat(detector.detect("더는 못 버티겠어")).isPresent();
        assertThat(detector.detect("살아갈 이유를 모르겠어")).isPresent();
    }

    @Test
    void 학대_신호를_감지한다() {
        assertThat(detector.detect("남편이 자꾸 때리고 욕해요")).isPresent();
        assertThat(detector.detect("아빠한테 맞고 살아요")).isPresent();
    }

    @Test
    void 공백_변형을_감지한다() {
        assertThat(detector.detect("죽 고 싶 어")).isPresent();
    }

    @Test
    void 카테고리를_함께_보고한다() {
        assertThat(detector.detect("죽고 싶어"))
                .hasValueSatisfying(signal ->
                        assertThat(signal.category()).isEqualTo("자살자해직접"));
    }

    @Test
    void 평범한_메시지는_감지하지_않는다() {
        assertThat(detector.detect("오늘 날씨가 좋네요")).isEmpty();
        assertThat(detector.detect("내일 면접인데 기도문 써줘")).isEmpty();
        assertThat(detector.detect("방언이 뭐야?")).isEmpty();
        assertThat(detector.detect("교회 안 간 지 1년 됐어")).isEmpty();
        assertThat(detector.detect("")).isEmpty();
        assertThat(detector.detect(null)).isEmpty();
    }

    @Test
    void 패턴이_비어_있으면_생성을_거부한다() {
        assertThatThrownBy(() -> new CrisisDetector(List.of("# 주석뿐")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 형식이_잘못된_패턴_줄은_생성을_거부한다() {
        assertThatThrownBy(() -> new CrisisDetector(List.of("탭없는줄")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
