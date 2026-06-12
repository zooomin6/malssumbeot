package com.malssumbeot.crisis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CrisisFilterTest {

    private final CrisisTestSupport.MutableClock clock = new CrisisTestSupport.MutableClock();
    private final CrisisFilter filter = new CrisisFilter(
            CrisisTestSupport.productionDetector(),
            new CrisisSessionStore(Duration.ofMinutes(30), clock));

    @Test
    void 위기_신호를_감지하면_위기로_판정한다() {
        CrisisCheck check = filter.check("s1", "죽고 싶어");

        assertThat(check.crisis()).isTrue();
        assertThat(check.trigger()).isEqualTo(CrisisCheck.Trigger.NEW_SIGNAL);
        assertThat(check.signal()).isPresent();
    }

    @Test
    void 위기_이후의_평범한_메시지도_위기_상태를_유지한다() {
        filter.check("s1", "다 끝내고 싶어");

        // theology-checker 지적 시나리오: 직전 턴 위기 → 현재 턴 평범한 요청
        CrisisCheck check = filter.check("s1", "내일 면접 기도문 써줘");

        assertThat(check.crisis()).isTrue();
        assertThat(check.trigger()).isEqualTo(CrisisCheck.Trigger.STICKY);
    }

    @Test
    void 유지_시간이_지나면_평범한_메시지는_위기가_아니다() {
        filter.check("s1", "다 끝내고 싶어");
        clock.advanceSeconds(31 * 60);

        CrisisCheck check = filter.check("s1", "내일 면접 기도문 써줘");

        assertThat(check.crisis()).isFalse();
        assertThat(check.trigger()).isEqualTo(CrisisCheck.Trigger.NONE);
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
}
