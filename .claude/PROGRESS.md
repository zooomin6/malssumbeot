# PROGRESS.md — 말씀벗 진행 상황

> 모든 에이전트 세션은 이 파일을 먼저 읽고, 작업 후 갱신한다.
> 마지막 갱신: 2026-06-12 (백엔드 기반 구축 1차)

## 현재 마일스톤: M1 — 기반 구축 (1~2주차)

## 완료된 일

- [x] 제품기획서 v1 작성 (`docs/PRD.md`)
- [x] CLAUDE.md (프로젝트 헌법) 작성
- [x] 신학 검사 에이전트 프롬프트 작성 (`agents/theology-checker.md`)
- [x] 메모리 파일 초기화 (PROGRESS.md, DECISIONS.md)
- [x] Spring Boot 3.5.15 프로젝트 스캐폴딩 (`backend/`, Maven, Java 17 타겟) — D-009
- [x] 성경 DB 스키마 (Flyway V1) + 66권 메타데이터 시드 (V2) — PostgreSQL에서 적용 검증 완료
- [x] BibleVerseService: 구절 주소 파싱(풀네임/약어/범위/장절 표기) → DB 원문 조회 +
      존재 검증(없는 구절 `VerseNotFoundException` 거부, T8 환각 방지 기반) — 단위 테스트 20건 통과
- [x] 개역한글 본문 TSV 임포터 (`bible-import` 프로파일) — 소스 확정 즉시 적재 가능
- [x] 로컬 개발용 PostgreSQL docker-compose (호스트 포트 55432 — 로컬 PG 18과 충돌 회피)

## 진행 중

- [ ] (없음 — 다음 작업 대기)

## 다음 작업 (우선순위순)

### 백엔드 (Spring Boot — 플랫폼 무관, 그대로 진행)
1. [ ] 개역한글 성경 텍스트 확보 → 임포터로 적재 (텍스트 소스 확정 블로킹, 아래 참조)
2. [ ] Claude API 연동 + 의도 분류기
   (상담/기도문/지식QA/일상대화/범위밖/위기 — 6분류)
3. [ ] CrisisFilter 구현 (의도 분류보다 앞단)
4. [ ] 시스템 프롬프트 5종 탑재 (기존 4종 + 일상 대화 모드, PRD §5 기준)
5. [ ] 모델 라우팅: 일상 대화=경량 모델, 신앙 상담·QA=상위 모델
6. [ ] QA 러너: T1~T8 자동 실행 → theology-checker 판정 → 리포트 저장
7. [ ] 채팅 REST API + 인증(카카오/구글/Apple 소셜 로그인) + FCM/APNs 푸시

### 모바일 (React Native + Expo)
8. [ ] Expo 프로젝트 스캐폴딩, 채팅 UI (메시지 리스트, 성경 구절 인용 블록 구분 렌더링)
9. [ ] 로그인 플로우 + 대화 이력 동기화
10. [ ] 오늘의 말씀 푸시 알림 (수신 동의 기반)
11. [ ] 스토어 제출 준비: 개인정보처리방침, AI 생성 콘텐츠 고지, 신고 버튼
    (앱 내 신고 → theology-checker 1차 분류 큐 연동)

## 사람 확인 필요 (블로킹)

- [ ] 개역한글 텍스트 소스 확정 (공개 텍스트 파일 출처 검증 필요)
  → 확정되면 TSV(`책약어<TAB>장<TAB>절<TAB>본문`)로 변환 후 `bible-import` 프로파일로 적재
    (`backend/README.md` 참조)
- [ ] CLAUDE.md 패키지 컨벤션의 `webhook` 모듈 명칭 확인: D-002(카톡 챗봇 → RN 앱)로
  webhook이 REST API로 대체되었으므로 `com.malssumbeot.api`로 명명 제안 (CLAUDE.md 수정 필요)
- [ ] Apple Developer($99/년) / Google Play($25) 개발자 계정 등록
- [ ] 소셜 로그인용 카카오/구글/Apple 개발자 콘솔 앱 등록
- [ ] 서비스명 최종 확정 (현재 가칭: 말씀벗) — 스토어 등록 전 필수
- [ ] RN 담당 협업자 확정 (친구 협업 vs 직접 개발)

## 루프 종료 조건 (Definition of Done)

CLAUDE.md의 DoD 체크리스트 참조. 전부 충족 시 베타 배포 보고.

## 세션 로그

| 날짜 | 작업 | 결과 |
|------|------|------|
| 2026-06-12 | 프로젝트 초기화, 뼈대 파일 4종 생성 | 완료 |
| 2026-06-12 | D-002 개정: 카톡 챗봇 → RN 앱 (민규 승인), 자유 대화 기능 추가 (D-008) | 완료 |
| 2026-06-12 | 백엔드 기반 구축 1차: Boot 3.5.15 스캐폴딩, 성경 스키마+66권 시드(Flyway), BibleVerseService+파서, TSV 임포터, docker-compose. 테스트 20건 통과, PostgreSQL 기동 검증 | 완료 |
