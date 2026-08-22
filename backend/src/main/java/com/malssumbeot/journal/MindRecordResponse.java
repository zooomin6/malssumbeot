package com.malssumbeot.journal;

import java.time.Instant;

public record MindRecordResponse(Long id, String mood, String content, Instant createdAt) {

    public static MindRecordResponse from(MindRecord record) {
        return new MindRecordResponse(record.getId(), record.getMood(), record.getContent(), record.getCreatedAt());
    }
}
