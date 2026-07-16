package com.malssumbeot.crisis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CrisisSessionStoreTest {

    private final CrisisTestSupport.MutableClock clock = new CrisisTestSupport.MutableClock();
    private final CrisisSessionStore store =
            new CrisisSessionStore(Duration.ofMinutes(30), clock);

    @Test
    void 마킹된_세션은_최고_강도다() {
        store.mark("session-1");

        assertThat(store.level("session-1")).isEqualTo(CrisisLevel.HIGH);
        assertThat(store.level("session-2")).isEqualTo(CrisisLevel.NONE);
    }

    @Test
    void 유지_시간마다_한_단계씩_내려간다() {
        store.mark("session-1");

        clock.advanceSeconds(31 * 60); // 1단위 경과
        assertThat(store.level("session-1")).isEqualTo(CrisisLevel.MID);

        clock.advanceSeconds(30 * 60); // 2단위 경과
        assertThat(store.level("session-1")).isEqualTo(CrisisLevel.LOW);

        clock.advanceSeconds(30 * 60); // 3단위 경과
        assertThat(store.level("session-1")).isEqualTo(CrisisLevel.NONE);
    }

    @Test
    void 재감지되면_최고_강도로_복귀한다() {
        store.mark("session-1");
        clock.advanceSeconds(31 * 60);
        assertThat(store.level("session-1")).isEqualTo(CrisisLevel.MID);

        store.mark("session-1"); // 새 신호
        assertThat(store.level("session-1")).isEqualTo(CrisisLevel.HIGH);
    }
}
