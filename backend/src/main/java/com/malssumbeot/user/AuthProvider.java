package com.malssumbeot.user;

/**
 * 소셜 로그인 제공자. MVP는 구글부터 시작하고(D-021), 카카오·애플은 이후 추가한다.
 * DB에는 이름(문자열)으로 저장한다 — 순서 변경에 안전하도록.
 *
 * DEV는 실제 소셜 로그인이 아니라 {@code dev} 프로파일 전용 테스트 계정 식별자다
 * (`DevAuthController` 참고, 모바일 서버 연결 검증용).
 */
public enum AuthProvider {
    GOOGLE,
    KAKAO,
    APPLE,
    DEV
}
