package com.malssumbeot.journal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 마음 기록 생성 요청. mood는 기분 칩에서 오면 채워지고, 자유 입력이면 null. */
public record MindRecordRequest(
        String mood,
        @NotBlank(message = "content는 비어 있을 수 없습니다")
        @Size(max = 1000, message = "content는 1000자를 넘을 수 없습니다") String content) {
}
