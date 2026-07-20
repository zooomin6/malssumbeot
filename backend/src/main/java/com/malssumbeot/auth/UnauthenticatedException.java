package com.malssumbeot.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 보호된 엔드포인트에 유효한 JWT 없이 접근했을 때 (헤더 없음/형식 오류/서명·만료 실패). HTTP 401로 매핑된다.
 * 제공자 토큰 검증 실패({@link InvalidSocialTokenException})와 구분한다 — 이쪽은 우리 자체 JWT 검사 실패다.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException(String message) {
        super(message);
    }

    public UnauthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
