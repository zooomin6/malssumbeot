-- 보관함: 사용자가 명시적으로 저장한 대화 한 턴(질문+바나바 답변+구절 스냅샷) (2026-08-17)
-- verses_snapshot은 저장 시점에 이미 BibleVerseService 검증을 통과한 원문의 JSON 스냅샷이다(D-025).
-- 전체 대화 자동 저장 금지(D-024)를 뒤집는 게 아니라, 사용자가 북마크를 누른 항목만 저장한다.

CREATE TABLE archive_items (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    user_message    VARCHAR(4000),
    barnabas_reply  VARCHAR(4000) NOT NULL,
    verses_snapshot TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_archive_items_user_id ON archive_items(user_id);
