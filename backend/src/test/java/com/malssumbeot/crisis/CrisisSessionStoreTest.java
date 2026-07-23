package com.malssumbeot.crisis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CrisisSessionStoreTest {

    private final CrisisTestSupport.MutableClock clock = new CrisisTestSupport.MutableClock();
    private final CrisisSessionStore store = new CrisisSessionStore(clock);

    @Test
    void 마킹된_세션은_sticky를_한_번_돌려준다() {
        store.mark("session-1", "학대");

        assertThat(store.consumeSticky("session-1")).contains(new CrisisSessionStore.CrisisMark("학대"));
    }

    @Test
    void 마킹되지_않은_세션은_비어있다() {
        assertThat(store.consumeSticky("session-2")).isEmpty();
    }

    @Test
    void sticky는_한_번_소비하면_사라진다() {
        store.mark("session-1", "자살자해직접");

        assertThat(store.consumeSticky("session-1")).isPresent();
        assertThat(store.consumeSticky("session-1")).isEmpty(); // 두 번째는 이미 소비됨
    }

    @Test
    void 카테고리_없이도_마킹할_수_있다() {
        store.mark("session-1", null);

        assertThat(store.consumeSticky("session-1")).contains(new CrisisSessionStore.CrisisMark(null));
    }

    @Test
    void 재마킹하면_새_카테고리로_덮어쓴다() {
        store.mark("session-1", "학대");
        store.mark("session-1", "자살자해직접");

        assertThat(store.consumeSticky("session-1")).contains(new CrisisSessionStore.CrisisMark("자살자해직접"));
    }

    @Test
    void 오래_방치된_세션은_정리_대상이지만_최근_마킹은_그대로_남는다() {
        store.mark("session-old", "학대");
        clock.advanceSeconds(25 * 3600); // 25시간 경과 — 정리 대상(24시간 초과)
        store.mark("session-new", "자살자해직접"); // 방금 마킹 — 정리 대상 아님

        store.evictStale();

        assertThat(store.consumeSticky("session-old")).isEmpty();
        assertThat(store.consumeSticky("session-new")).isPresent();
    }
}
