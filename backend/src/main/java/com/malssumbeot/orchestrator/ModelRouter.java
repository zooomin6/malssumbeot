package com.malssumbeot.orchestrator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 의도별 모델 라우팅 (PRD §6.2 단위 경제성: Haiku/Sonnet 혼용, D-013).
 * 신앙 상담·QA·기도문·위기 = 상위 모델, 일상 대화·범위 밖 = 경량 모델.
 */
@Component
public class ModelRouter {

    private final String faithModel;
    private final String casualModel;

    public ModelRouter(@Value("${malssumbeot.anthropic.faith-model}") String faithModel,
                       @Value("${malssumbeot.anthropic.casual-model}") String casualModel) {
        this.faithModel = faithModel;
        this.casualModel = casualModel;
    }

    public String route(Intent intent) {
        return switch (intent) {
            case CRISIS, COUNSELING, PRAYER, KNOWLEDGE_QA -> faithModel;
            case DAILY_CHAT, OUT_OF_SCOPE -> casualModel;
        };
    }
}
