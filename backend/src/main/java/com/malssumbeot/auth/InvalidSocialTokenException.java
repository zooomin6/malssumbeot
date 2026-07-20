package com.malssumbeot.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 제공자 토큰이 유효하지 않거나 검증에 실패했을 때. HTTP 401로 매핑된다. */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidSocialTokenException extends RuntimeException {

    public InvalidSocialTokenException(String message) {
        super(message);
    }

    public InvalidSocialTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
