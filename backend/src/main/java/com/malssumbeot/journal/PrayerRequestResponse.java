package com.malssumbeot.journal;

import java.time.Instant;

public record PrayerRequestResponse(Long id, String content, Instant createdAt) {

    public static PrayerRequestResponse from(PrayerRequest request) {
        return new PrayerRequestResponse(request.getId(), request.getContent(), request.getCreatedAt());
    }
}
