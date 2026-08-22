package com.malssumbeot.journal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 본인 소유가 아니거나 존재하지 않는 기도 제목에 접근했을 때. HTTP 404로 매핑된다. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PrayerRequestNotFoundException extends RuntimeException {

    public PrayerRequestNotFoundException(String message) {
        super(message);
    }
}
