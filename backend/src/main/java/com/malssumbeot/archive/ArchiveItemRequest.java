package com.malssumbeot.archive;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 보관함 저장 요청. userMessage는 바나바 답변이 초기 안내문일 때 없을 수 있어 nullable. */
public record ArchiveItemRequest(
        @Size(max = 4000, message = "userMessage는 4000자를 넘을 수 없습니다") String userMessage,
        @NotBlank(message = "barnabasReply는 비어 있을 수 없습니다")
        @Size(max = 4000, message = "barnabasReply는 4000자를 넘을 수 없습니다") String barnabasReply,
        List<VerseSnapshot> verses) {

    public ArchiveItemRequest {
        if (verses == null) {
            verses = List.of();
        }
    }
}
