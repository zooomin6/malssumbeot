-- 마음 기록: 홈 화면 기분 칩 선택 또는 자유 입력 (2026-08-17)
-- 사용자가 명시적으로 남긴 항목만 저장한다 — 대화 전체 자동 저장 금지(D-024)는 유지되며,
-- 이 저장은 그 원칙의 옵트인 예외다.

CREATE TABLE mind_records (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    mood       VARCHAR(50),
    content    VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_mind_records_user_id ON mind_records(user_id);
