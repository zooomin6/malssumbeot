package com.malssumbeot.auth;

import com.malssumbeot.user.AuthProvider;

/**
 * 제공자별 토큰 검증기 (방식 A, D-022). 구현체는 앱이 보낸 토큰의 진위를 확인하고 신원을 돌려준다.
 * 검증 실패는 {@link InvalidSocialTokenException}으로 던진다.
 */
public interface SocialTokenVerifier {

    /** 이 검증기가 담당하는 제공자. AuthService가 제공자→검증기 매핑을 만들 때 쓴다. */
    AuthProvider provider(); //이런 메서드를 반드시 만들어라라는 약속 , 인터페이스

    SocialUser verify(String token);
}
