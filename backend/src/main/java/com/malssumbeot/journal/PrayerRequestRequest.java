package com.malssumbeot.journal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 기도 제목 생성 요청. */
public record PrayerRequestRequest(
        @NotBlank(message = "content는 비어 있을 수 없습니다")
        @Size(max = 1000, message = "content는 1000자를 넘을 수 없습니다") String content) {
}
