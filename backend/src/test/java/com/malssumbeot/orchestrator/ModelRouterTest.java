package com.malssumbeot.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ModelRouterTest {

    private final ModelRouter router = new ModelRouter("sonnet-test", "haiku-test");

    @Test
    void 모든_의도가_경량_모델로_라우팅된다() {
        assertThat(router.route(Intent.CRISIS)).isEqualTo("haiku-test");
        assertThat(router.route(Intent.COUNSELING)).isEqualTo("haiku-test");
        assertThat(router.route(Intent.PRAYER)).isEqualTo("haiku-test");
        assertThat(router.route(Intent.KNOWLEDGE_QA)).isEqualTo("haiku-test");
        assertThat(router.route(Intent.DAILY_CHAT)).isEqualTo("haiku-test");
        assertThat(router.route(Intent.OUT_OF_SCOPE)).isEqualTo("haiku-test");
    }
}
