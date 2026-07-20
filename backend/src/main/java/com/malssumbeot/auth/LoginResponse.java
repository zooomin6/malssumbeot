package com.malssumbeot.auth;

import com.malssumbeot.user.User;

/**
 * 소셜 로그인 응답 DTO. accessToken은 우리 자체 JWT로, 앱이 이후 모든 API 호출에 사용한다.
 * nickname·email은 제공자 동의 범위에 따라 null일 수 있다.
 */
public record LoginResponse(String accessToken, String provider, String nickname, String email) {

    public static LoginResponse from(User user, String accessToken) {
        return new LoginResponse(accessToken, user.getProvider().name(),
                user.getNickname(), user.getEmail());
    }
}
