package com.malssumbeot.api;

import jakarta.validation.constraints.NotBlank;

/**
 * 채팅 요청 DTO.
 *
 * sessionId는 위기 sticky가 세션 단위(D-012/D-020)라 필수다 — 클라이언트(RN 앱)가 안정적인
 * 세션/기기 식별자를 제공한다. 인증(Phase 2) 도입 전까지는 바디로 받는다.
 */
public record ChatRequest(
        @NotBlank(message = "sessionId는 필수입니다") String sessionId,
        @NotBlank(message = "message는 비어 있을 수 없습니다") String message) {
}
