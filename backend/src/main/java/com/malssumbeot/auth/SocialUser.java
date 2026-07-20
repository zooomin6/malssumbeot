package com.malssumbeot.auth;

import com.malssumbeot.user.AuthProvider;

/**
 * 제공자 토큰 검증으로 확인된 신원. providerId는 제공자 안에서 사용자를 유일하게 식별한다.
 * email·nickname은 제공자 동의 범위에 따라 없을 수 있다 (예: 카카오 이메일은 비즈앱 전 미제공).
 */
public record SocialUser(AuthProvider provider, String providerId, String email, String nickname) {
}
