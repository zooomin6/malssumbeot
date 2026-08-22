package com.malssumbeot.archive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 사용자가 명시적으로 저장한 대화 한 턴(질문+바나바 답변+구절 스냅샷).
 *
 * 대화 전체 자동 저장 금지(D-024)를 뒤집는 게 아니다 — 사용자가 북마크를 누른 항목만 저장한다.
 * versesSnapshot은 저장 시점에 이미 BibleVerseService 검증을 통과한 원문의 JSON 스냅샷이다(D-025).
 */
@Entity
@Table(name = "archive_items")
public class ArchiveItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_message", length = 4000)
    private String userMessage;

    @Column(name = "barnabas_reply", nullable = false, length = 4000)
    private String barnabasReply;

    @Column(name = "verses_snapshot", columnDefinition = "TEXT")
    private String versesSnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ArchiveItem() {
    }

    public ArchiveItem(Long userId, String userMessage, String barnabasReply, String versesSnapshot) {
        this.userId = userId;
        this.userMessage = userMessage;
        this.barnabasReply = barnabasReply;
        this.versesSnapshot = versesSnapshot;
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

    public String getUserMessage() {
        return userMessage;
    }

    public String getBarnabasReply() {
        return barnabasReply;
    }

    public String getVersesSnapshot() {
        return versesSnapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
