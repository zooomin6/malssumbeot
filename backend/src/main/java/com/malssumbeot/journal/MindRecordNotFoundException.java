package com.malssumbeot.journal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 본인 소유가 아니거나 존재하지 않는 마음 기록에 접근했을 때. HTTP 404로 매핑된다. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class MindRecordNotFoundException extends RuntimeException {

    public MindRecordNotFoundException(String message) {
        super(message);
    }
}
