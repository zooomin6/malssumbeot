package com.malssumbeot.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 소셜 로그인 제공자(카카오/구글) 서버 장애·타임아웃·네트워크 실패로 토큰 검증 자체를
 * 수행하지 못했을 때. 토큰이 실제로 무효한 것과는 다른 상황이라 401이 아닌 503으로 매핑한다.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class SocialProviderUnavailableException extends RuntimeException {

    public SocialProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
