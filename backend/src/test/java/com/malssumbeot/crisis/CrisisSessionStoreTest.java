package com.malssumbeot.crisis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CrisisSessionStoreTest {

    private final CrisisTestSupport.MutableClock clock = new CrisisTestSupport.MutableClock();
    private final CrisisSessionStore store =
            new CrisisSessionStore(Duration.ofMinutes(30), clock);

    @Test
    void 마킹된_세션은_위기_상태다() {
        store.mark("session-1");

        assertThat(store.isActive("session-1")).isTrue();
        assertThat(store.isActive("session-2")).isFalse();
    }

    @Test
    void 유지_시간이_지나면_해제된다() {
        store.mark("session-1");
        clock.advanceSeconds(31 * 60);

        assertThat(store.isActive("session-1")).isFalse();
    }

    @Test
    void 재감지되면_유지_시간이_갱신된다() {
        store.mark("session-1");
        clock.advanceSeconds(20 * 60);
        store.mark("session-1");
        clock.advanceSeconds(20 * 60);

        assertThat(store.isActive("session-1")).isTrue();
    }
}
