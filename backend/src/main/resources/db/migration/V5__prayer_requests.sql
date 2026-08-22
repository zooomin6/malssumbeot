-- 기도 제목: 홈 화면 자유 입력 (2026-08-17). mind_records와 같은 취지의 옵트인 저장.

CREATE TABLE prayer_requests (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    content    VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_prayer_requests_user_id ON prayer_requests(user_id);
