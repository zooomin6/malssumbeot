package com.malssumbeot.orchestrator;

/**
 * Claude Messages API의 얇은 추상화. 호출부가 SDK 타입에 직접 의존하지 않게 하여
 * 단위 테스트(모킹)와 모델 교체를 쉽게 한다.
 */
public interface ClaudeChat {

    /**
     * 단일 사용자 메시지에 대한 텍스트 응답을 반환한다.
     */
    String complete(String model, int maxTokens, String systemPrompt, String userMessage);
}
