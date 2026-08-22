package com.malssumbeot.archive;

import java.time.Instant;
import java.util.List;

public record ArchiveItemResponse(
        Long id, String userMessage, String barnabasReply, List<VerseSnapshot> verses, Instant createdAt) {
}
