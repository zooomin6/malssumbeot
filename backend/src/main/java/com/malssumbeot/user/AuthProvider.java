package com.malssumbeot.user;

/**
 * 소셜 로그인 제공자. MVP는 구글부터 시작하고(D-021), 카카오·애플은 이후 추가한다.
 * DB에는 이름(문자열)으로 저장한다 — 순서 변경에 안전하도록.
 */
public enum AuthProvider {
    GOOGLE,
    KAKAO,
    APPLE
}
