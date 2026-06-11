# PROGRESS.md — 말씀벗 진행 상황

> 모든 에이전트 세션은 이 파일을 먼저 읽고, 작업 후 갱신한다.
> 마지막 갱신: 2026-06-12 (Claude API 연동 + 의도 분류기)

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
- [x] Anthropic Java SDK(2.40.1) 연동 — `AnthropicClient` 빈, API 키는 env var(미설정 시 부팅은 가능)
- [x] 의도 분류기 6분류 (상담/기도문/지식QA/일상대화/범위밖/위기) — 경량 모델 `claude-haiku-4-5` (D-010)
- [x] 신학 검사 에이전트 실행(분류 프롬프트 검토) → **FAIL(critical)**: 위기 감지 후 형식 위반 시
      상담으로 강등되는 파싱 경로 지적 → "위기" 부분 일치 우선 파싱으로 코드 수정 완료 (D-011).
      프롬프트 본문 수정안 3건은 사람 승인 대기 (아래 "사람 확인 필요")
- [x] 테스트 29건 통과

## 진행 중

- [ ] (없음 — 다음 작업 대기)

## 다음 작업 (우선순위순)

### 백엔드 (Spring Boot — 플랫폼 무관, 그대로 진행)
1. [ ] 개역한글 성경 텍스트 확보 → 임포터로 적재 (텍스트 소스 확정 블로킹, 아래 참조)
2. [ ] CrisisFilter 구현 (의도 분류보다 앞단)
   - theology-checker 지적 반영: 세션 단위 위기 상태 유지(sticky crisis flag) 포함 —
     직전 턴에 위기 신호가 있었으면 이후 턴도 위기 프로토콜 유지
3. [ ] 시스템 프롬프트 5종 탑재 (기존 4종 + 일상 대화 모드, PRD §5 기준)
4. [ ] 모델 라우팅: 일상 대화=경량 모델, 신앙 상담·QA=상위 모델
5. [ ] QA 러너: T1~T8 자동 실행 → theology-checker 판정 → 리포트 저장
6. [ ] 채팅 REST API + 인증(카카오/구글/Apple 소셜 로그인) + FCM/APNs 푸시

### 모바일 (React Native + Expo)
7. [ ] Expo 프로젝트 스캐폴딩, 채팅 UI (메시지 리스트, 성경 구절 인용 블록 구분 렌더링)
8. [ ] 로그인 플로우 + 대화 이력 동기화
9. [ ] 오늘의 말씀 푸시 알림 (수신 동의 기반)
10. [ ] 스토어 제출 준비: 개인정보처리방침, AI 생성 콘텐츠 고지, 신고 버튼
    (앱 내 신고 → theology-checker 1차 분류 큐 연동)

## 사람 확인 필요 (블로킹)

- [ ] 개역한글 텍스트 소스 확정 (공개 텍스트 파일 출처 검증 필요)
  → 확정되면 TSV(`책약어<TAB>장<TAB>절<TAB>본문`)로 변환 후 `bible-import` 프로파일로 적재
    (`backend/README.md` 참조)
- [ ] CLAUDE.md 패키지 컨벤션의 `webhook` 모듈 명칭 확인: D-002(카톡 챗봇 → RN 앱)로
  webhook이 REST API로 대체되었으므로 `com.malssumbeot.api`로 명명 제안 (CLAUDE.md 수정 필요)
- [ ] ANTHROPIC_API_KEY 발급·설정 (외부 API 키 = 사람 승인 항목). 키 설정 후
  분류기 실 호출 스모크 테스트 1회 필요
- [ ] **의도 분류 프롬프트 승인** (`backend/src/main/resources/prompts/intent-classifier.txt`):
  신규 작성 프롬프트이므로 검토 요청. theology-checker(2026-06-12, critical FAIL)의
  프롬프트 수정안 3건도 함께 승인 필요 — 승인 전에는 반영하지 않음:
  1. 위기 범주에 제3자 위기 신호 추가 ("친구가 죽고 싶대요", 타인 위해 위협)
  2. 위기 범주에 혼합 예시 추가 ("다른 요청 속에 끼어든 위기 표현 포함")
  3. 기도문 범주에 경계 문구 추가 ("질병의 치유 여부를 묻는 질문은 기도문이 아님" —
     T4가 기도문 분기로 끌려가 치유 확언 위험 방지)
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
| 2026-06-12 | GitHub 레포 생성(zooomin6/malssumbeot, private) + 첫 커밋·푸시 (main) | 완료 |
| 2026-06-12 | Anthropic SDK 연동 + 의도 분류기 6분류(Haiku). 신학 검사 실행 → critical 지적(위기 강등 경로) 코드로 수정, 프롬프트 수정안은 승인 대기. 테스트 29건 통과 | 완료 |
