# 말씀벗 백엔드

Spring Boot 3.x + PostgreSQL + Claude API. 전체 기획은 [PRD](../.claude/docs/PRD.md), 프로젝트 원칙은 [CLAUDE.md](../.claude/CLAUDE.md) 참조.

## 로컬 실행

```bash
# 1. PostgreSQL 기동 (레포 루트에서)
docker compose up -d

# 2. 서버 실행 (Flyway가 스키마 + 66권 메타데이터 자동 적용)
cd backend
./mvnw spring-boot:run
```

## 테스트

```bash
./mvnw test
```

## 성경 본문 적재 (소스 확정 후)

개역한글(1961) 텍스트 소스가 확정되면(PROGRESS.md "사람 확인 필요" 참조)
TSV 파일(`책약어<TAB>장<TAB>절<TAB>본문`, UTF-8)을 준비하고:

```bash
BIBLE_IMPORT_FILE=/path/to/krv.tsv ./mvnw spring-boot:run -Dspring-boot.run.profiles=bible-import
```

이미 본문이 적재된 DB에서는 중복 적재를 막기 위해 임포터가 중단된다.

## 핵심 설계 제약 (위반 금지)

- 성경 본문은 `BibleVerseService`가 조회한 DB 원문만 사용자에게 전달한다.
  모델이 생성한 구절 본문을 그대로 내보내는 코드 경로 금지 (D-003).
- DB에 없는 구절 주소는 `VerseNotFoundException`으로 거부한다 (환각 방지, QA T8).
- 위기 감지(`CrisisFilter`)는 의도 분류보다 앞단에 둔다 (D-004) — M1 후속 작업.
