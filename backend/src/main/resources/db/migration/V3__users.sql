-- 소셜 로그인 사용자 (Phase 2, D-021)
-- 인증 신원만 저장. 신앙 설문·대화 이력은 정책/설계 확정 후 별도 마이그레이션으로 추가.
-- 테이블명은 예약어(user)를 피해 users.

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    provider    VARCHAR(20)  NOT NULL,        -- GOOGLE / KAKAO / APPLE (AuthProvider)
    provider_id VARCHAR(255) NOT NULL,        -- 제공자가 주는 고유 식별자 (구글 sub 등)
    email       VARCHAR(320),                 -- 제공자에 따라 없을 수 있음
    nickname    VARCHAR(100),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_user_provider UNIQUE (provider, provider_id)
);
