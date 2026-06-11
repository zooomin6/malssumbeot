-- 성경 DB 스키마 (D-001: 개역한글 1961, 저작권 만료 역본만 적재)
-- D-003: 모든 구절 인용은 이 테이블의 원문만 사용한다.

CREATE TABLE bible_book (
    id            INT PRIMARY KEY,              -- 1~66, 정경 순서
    code          VARCHAR(8)  NOT NULL UNIQUE,  -- 영문 코드 (gen, exo, ...)
    name_ko       VARCHAR(20) NOT NULL UNIQUE,  -- 풀네임 (창세기)
    abbr_ko       VARCHAR(8)  NOT NULL UNIQUE,  -- 한국 교회 표준 약어 (창)
    testament     VARCHAR(2)  NOT NULL CHECK (testament IN ('OT', 'NT')),
    chapter_count INT         NOT NULL CHECK (chapter_count > 0)
);

CREATE TABLE bible_verse (
    id      BIGSERIAL PRIMARY KEY,
    book_id INT  NOT NULL REFERENCES bible_book (id),
    chapter INT  NOT NULL CHECK (chapter > 0),
    verse   INT  NOT NULL CHECK (verse > 0),
    text    TEXT NOT NULL,
    CONSTRAINT uq_bible_verse UNIQUE (book_id, chapter, verse)
);

-- 구절 주소 조회는 uq_bible_verse 인덱스로 충분.
-- 주제/키워드 검색(RAG)용 인덱스는 임베딩 검색 설계 시 별도 마이그레이션으로 추가한다.
