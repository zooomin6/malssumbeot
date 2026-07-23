package com.malssumbeot.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 채팅 요청 DTO.
 *
 * sessionId는 위기 sticky가 세션 단위(D-012/D-026)라 필수다 — 클라이언트(RN 앱)가 안정적인
 * 세션/기기 식별자를 제공한다. 인증은 JWT(D-023, WebConfig)로 처리하며, sessionId는 그와 별개로
 * 여전히 바디로 받는다(기기/세션 단위 sticky 식별자이기 때문).
 */
public record ChatRequest(
        @NotBlank(message = "sessionId는 필수입니다") String sessionId,
        @NotBlank(message = "message는 비어 있을 수 없습니다")
        @Size(max = 4000, message = "message는 4000자를 넘을 수 없습니다") String message) {
}
