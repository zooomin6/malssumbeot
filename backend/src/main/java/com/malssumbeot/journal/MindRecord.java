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
 * 홈 화면 기분 칩 선택 또는 자유 입력으로 남기는 짧은 마음 기록.
 *
 * 사용자가 명시적으로 남긴 항목만 저장한다 — 대화 전체 자동 저장 금지(D-024)는 유지되며,
 * 이 저장은 그 원칙의 옵트인 예외다.
 */
@Entity
@Table(name = "mind_records")
public class MindRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "mood", length = 50)
    private String mood;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MindRecord() {
    }

    public MindRecord(Long userId, String mood, String content) {
        this.userId = userId;
        this.mood = mood;
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

    public String getMood() {
        return mood;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
