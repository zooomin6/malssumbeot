package com.malssumbeot.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * 소셜 로그인 사용자. 인증 신원만 담는다 — 한 사람은 (provider, providerId)로 유일하게 식별된다.
 *
 * 신앙 설문(신앙 시작 시기 등)과 대화 이력은 아직 담지 않는다: 전자는 회원가입 설문 설계(Phase 3),
 * 후자는 위기·학대 진술 데이터 보관 정책(법률 검토) 확정 후에 별도로 붙인다.
 *
 * 테이블명은 예약어를 피해 users. email·nickname은 제공자에 따라 없을 수 있어 nullable.
 */
@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_provider",
                columnNames = {"provider", "provider_id"}))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(AuthProvider provider, String providerId, String email, String nickname) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.nickname = nickname;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
