package com.malssumbeot.journal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 홈 화면에서 자유 입력으로 남기는 기도 제목.
 *
 * 사용자가 명시적으로 남긴 항목만 저장한다 — 대화 전체 자동 저장 금지(D-024)는 유지되며,
 * 이 저장은 그 원칙의 옵트인 예외다.
 */
@Entity
@Table(name = "prayer_requests")
public class PrayerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PrayerRequest() {
    }

    public PrayerRequest(Long userId, String content) {
        this.userId = userId;
        this.content = content;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
