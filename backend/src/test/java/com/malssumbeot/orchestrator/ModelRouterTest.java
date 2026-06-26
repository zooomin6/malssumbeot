package com.malssumbeot.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ModelRouterTest {

    private final ModelRouter router = new ModelRouter("sonnet-test", "haiku-test");

    @Test
    void 신앙_관련_의도는_상위_모델로_라우팅한다() {
        assertThat(router.route(Intent.CRISIS)).isEqualTo("sonnet-test");
        assertThat(router.route(Intent.COUNSELING)).isEqualTo("sonnet-test");
        assertThat(router.route(Intent.PRAYER)).isEqualTo("sonnet-test");
        assertThat(router.route(Intent.KNOWLEDGE_QA)).isEqualTo("sonnet-test");
    }

    @Test
    void 일상_대화와_범위_밖은_경량_모델로_라우팅한다() {
        assertThat(router.route(Intent.DAILY_CHAT)).isEqualTo("haiku-test");
        assertThat(router.route(Intent.OUT_OF_SCOPE)).isEqualTo("haiku-test");
    }
}
