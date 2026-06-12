# 03. 성경 DB와 Flyway 마이그레이션

## 1. Flyway: 스키마도 버전 관리한다

코드가 git으로 버전 관리되듯, DB 스키마는 Flyway 마이그레이션으로 버전 관리한다.

```
src/main/resources/db/migration/
 ├─ V1__bible_schema.sql        # 테이블 생성
 └─ V2__seed_bible_books.sql    # 66권 메타데이터 시드
```

- 파일명 규칙: `V버전__설명.sql` (언더스코어 2개). 부팅 시 Flyway가 순서대로 실행한다.
- 실행 이력은 DB의 `flyway_schema_history` 테이블에 남는다. **한 번 적용된 마이그레이션
  파일은 수정하면 안 된다** (체크섬 불일치로 부팅 실패) — 바꿀 게 있으면 V3을 새로 만든다.
- Hibernate `ddl-auto: validate`와 짝을 이룬다: Flyway가 만들고, Hibernate가 검증한다.
  엔티티만 고치고 마이그레이션을 깜빡하면 부팅이 실패해서 바로 잡힌다.

## 2. 스키마 설계 결정들

```sql
CREATE TABLE bible_book (
    id            INT PRIMARY KEY,              -- 1~66, 정경 순서
    code          VARCHAR(8)  NOT NULL UNIQUE,  -- gen, exo, ...
    name_ko       VARCHAR(20) NOT NULL UNIQUE,  -- 창세기
    abbr_ko       VARCHAR(8)  NOT NULL UNIQUE,  -- 창
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
```

- **책 메타와 본문을 분리** — 책 이름·약어·장 수는 본문 없이도 쓸모가 있다
  (구절 주소 파싱, "빌립보서는 4장까지" 검증). 실제로 본문 적재 전인 지금도 동작한다.
- **`UNIQUE (book_id, chapter, verse)`가 인덱스를 겸한다** — 구절 조회는 항상
  (책, 장, 절 범위)로 들어오므로 이 복합 유니크 제약 하나로 무결성과 조회 성능을 동시에 챙긴다.
- **`chapter_count`를 메타에 둔 이유** — "빌립보서 99:1" 같은 요청을 DB 본문 조회 *전에*
  거부할 수 있다. 환각 검증의 1차 관문 (04장).
- **CHECK 제약** — 애플리케이션 버그로 0장·음수 절이 들어오는 것을 DB 레벨에서 차단.
  "검증은 가능한 한 데이터에 가까운 곳에서"가 원칙.

## 3. 왜 본문(verse)은 아직 비어 있나

개역한글(1961)은 저작권이 만료된 역본이지만, 인터넷에 도는 텍스트 파일이 "정말 개역한글
원문인지"는 검증이 필요하다. 잘못된 본문을 넣으면 이 서비스의 존재 이유가 무너진다.
그래서 **출처 확정은 사람(민규) 승인 항목**으로 막아두고, 확정 즉시 적재할 수 있게
TSV 임포터(`bible-import` 프로파일)만 준비해 뒀다. "할 수 있는 것과 해도 되는 것의 분리".

## 4. 로컬 PostgreSQL: docker-compose와 포트 충돌

```yaml
ports:
  - "55432:5432"   # 호스트 55432 → 컨테이너 5432
```

처음엔 5432:5432로 띄웠는데 앱이 **비밀번호 인증 실패**를 냈다. 원인 분석 과정:

1. `netstat -ano | grep 5432` → 5432 포트에 프로세스가 **둘** (docker-proxy + 또 하나)
2. PID 조회 → 로컬에 PostgreSQL 18이 Windows 서비스로 설치되어 있었음
3. 앱의 `localhost:5432` 접속이 컨테이너가 아니라 **로컬 PG 18로 연결**되고 있었던 것

교훈: "인증 실패"라는 에러 메시지가 항상 비밀번호 문제는 아니다. **엉뚱한 서버에 접속하고
있을 가능성**부터 확인하자. 해결은 기존 설치를 건드리지 않고 컨테이너 포트를 옮기는 것
(시스템 상태 변경 최소화).

## 5. 테스트 DB 전략: H2 인메모리

운영은 PostgreSQL, 테스트는 H2 인메모리를 쓴다 (`src/test/resources/application.yml`):

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:malssumbeot;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
  jpa:
    hibernate:
      ddl-auto: create-drop   # 테스트는 엔티티에서 스키마 생성
  flyway:
    enabled: false            # 테스트에서는 끔
```

왜 테스트에서 Flyway를 끄나? V1 SQL은 PostgreSQL 방언(`BIGSERIAL`, `TEXT`)이라
H2에서 그대로 돌지 않는다. 대신 Hibernate가 엔티티에서 스키마를 생성(`create-drop`)한다.
트레이드오프: **마이그레이션 SQL 자체는 단위 테스트에서 검증되지 않는다** → 그래서
실제 PostgreSQL 컨테이너에 앱을 띄워 Flyway 적용을 별도로 확인했다.
(더 발전시키면 Testcontainers로 테스트에서도 진짜 PostgreSQL을 쓸 수 있다 — 추후 과제.)

이 호환성 때문에 엔티티에서 `columnDefinition = "text"` 같은 DB 종속 정의를 피하고
`@Column(length = 2000)`처럼 양쪽에서 동작하는 방식을 골랐다.
