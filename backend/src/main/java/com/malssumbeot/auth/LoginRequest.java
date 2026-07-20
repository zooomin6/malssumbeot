package com.malssumbeot.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 소셜 로그인 요청 DTO. token은 앱이 제공자(구글/카카오) SDK로 받은 토큰이다
 * (구글=ID 토큰, 카카오=액세스 토큰). 제공자는 경로 변수로 받는다.
 */
public record LoginRequest(@NotBlank(message = "token은 필수입니다") String token) {
}
