package com.malssumbeot.orchestrator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 의도별 모델 라우팅. 비용 절감을 위해 전부 경량 모델(Haiku)로 라우팅한다
 * (2026-08-15, D-013 상위 모델 혼용 잠정 보류). 유료 요금제 도입 시 유료 사용자에 한해
 * faithModel(Sonnet)로 전환하는 방식을 검토한다 — 그때까지 faithModel은 미사용.
 */
@Component
public class ModelRouter {

    private final String casualModel;

    public ModelRouter(@Value("${malssumbeot.anthropic.faith-model}") String faithModel,
                       @Value("${malssumbeot.anthropic.casual-model}") String casualModel) {
        this.casualModel = casualModel;
    }

    public String route(Intent intent) {
        return casualModel;
    }
}
