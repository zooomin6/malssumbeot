package com.malssumbeot.crisis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CrisisFilterTest {

    private final CrisisFilter filter = new CrisisFilter(
            CrisisTestSupport.productionDetector(), new CrisisSessionStore());

    @Test
    void 위기_신호를_감지하면_위기로_판정한다() {
        CrisisCheck check = filter.check("s1", "죽고 싶어");

        assertThat(check.crisis()).isTrue();
        assertThat(check.trigger()).isEqualTo(CrisisCheck.Trigger.NEW_SIGNAL);
        assertThat(check.signal()).isPresent();
    }

    @Test
    void 위기_직후_바로_다음_메시지_한_번은_평범해도_위기_상태를_유지한다() {
        filter.check("s1", "다 끝내고 싶어");

        // theology-checker 지적 시나리오: 직전 턴 위기 → 현재 턴 평범한 요청
        CrisisCheck check = filter.check("s1", "내일 면접 기도문 써줘");

        assertThat(check.crisis()).isTrue();
        assertThat(check.trigger()).isEqualTo(CrisisCheck.Trigger.STICKY);
    }

    @Test
    void sticky는_한_번_소비되면_그_다음_메시지부터는_새_요청대로_처리된다() {
        filter.check("s1", "다 끝내고 싶어"); // 위기 신호
        filter.check("s1", "내일 면접 기도문 써줘"); // sticky 1회 소비(D-026)

        CrisisCheck third = filter.check("s1", "오늘 뭐 먹을지 고민이야"); // 세 번째 메시지

        assertThat(third.crisis()).isFalse();
        assertThat(third.trigger()).isEqualTo(CrisisCheck.Trigger.NONE);
    }

    @Test
    void 다른_세션에는_영향을_주지_않는다() {
        filter.check("s1", "죽고 싶어");

        CrisisCheck check = filter.check("s2", "오늘 날씨 좋다");

        assertThat(check.crisis()).isFalse();
    }

    @Test
    void 평범한_세션은_위기가_아니다() {
        CrisisCheck check = filter.check("s1", "방언이 뭐야?");

        assertThat(check.crisis()).isFalse();
        assertThat(check.trigger()).isEqualTo(CrisisCheck.Trigger.NONE);
    }

    @Test
    void sticky_상태에서도_카테고리가_유지된다() {
        filter.check("s1", "아빠가 나를 때렸어"); // 학대 신호

        CrisisCheck check = filter.check("s1", "오늘 좀 그냥 힘든 얘기 하고 싶어");

        assertThat(check.trigger()).isEqualTo(CrisisCheck.Trigger.STICKY);
        assertThat(check.signal()).isPresent();
        assertThat(check.signal().get().category()).startsWith("학대");
    }
}
