package com.malssumbeot.orchestrator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 사용자 메시지 의도 분류 (경량 모델 사용).
 *
 * 주의: 이 분류기의 위기 감지는 보조 수단이다. 1차 위기 감지는 의도 분류보다
 * 앞단의 CrisisFilter가 담당한다 (D-004). 이 분기를 우회하는 설계 금지.
 */
@Component
public class IntentClassifier {

    private static final Logger log = LoggerFactory.getLogger(IntentClassifier.class);

    /** 분류 실패 시 폴백 — 상담 모드가 가장 보수적(공감 우선 + 성경 DB 근거)이다. */
    static final Intent FALLBACK_INTENT = Intent.COUNSELING;

    private final ClaudeChat claudeChat;
    private final String model;
    private final String systemPrompt;

    public IntentClassifier(ClaudeChat claudeChat,
                            @Value("${malssumbeot.anthropic.classifier-model}") String model) {
        this.claudeChat = claudeChat;
        this.model = model;
        this.systemPrompt = loadPrompt();
    }

    public Intent classify(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Intent.DAILY_CHAT;
        }
        String label = claudeChat.complete(model, 16, systemPrompt, userMessage);

        // 위기 강등 방지(D-004, theology-checker 2026-06-12 critical 지적):
        // 모델이 위기를 감지하고도 출력 형식을 어긴 경우(설명 덧붙임 등)
        // 정확 일치 파싱에 실패해 폴백으로 강등되는 경로를 차단한다.
        if (label != null && label.contains(Intent.CRISIS.label())) {
            return Intent.CRISIS;
        }
        return Intent.fromLabel(label).orElseGet(() -> {
            log.warn("의도 분류 결과를 해석할 수 없어 폴백합니다: '{}'", label);
            return FALLBACK_INTENT;
        });
    }

    private static String loadPrompt() {
        try {
            return new ClassPathResource("prompts/intent-classifier.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("의도 분류 프롬프트를 읽을 수 없습니다", e);
        }
    }
}
