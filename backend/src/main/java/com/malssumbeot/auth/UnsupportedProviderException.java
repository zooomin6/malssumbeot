package com.malssumbeot.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 등록된 검증기가 없는 제공자로 로그인을 시도했을 때 (예: 오타, 미구현 APPLE). HTTP 400으로 매핑된다. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedProviderException extends RuntimeException {

    public UnsupportedProviderException(String provider) {
        super("지원하지 않는 로그인 제공자입니다: " + provider);
    }
}
