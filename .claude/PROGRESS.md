# PROGRESS.md — 말씀벗 진행 상황

> 모든 에이전트 세션은 이 파일을 먼저 읽고, 작업 후 갱신한다.
> 마지막 갱신: 2026-07-16 (Phase 2 착수: User 엔티티+V3 마이그레이션, 서비스명 확정 D-021, 테스트 87건)

## 현재 마일스톤: M1 — 기반 구축 (1~2주차)

## 백엔드 진행 상황 (Entity → Repository → DTO → Service → Controller → Filter/Interceptor)

> 완료(`[x]`)와 다음 작업(`[ ]`)을 레이어 하나로 통합했다. 레이어 안에서는 도메인
> (bible/crisis/orchestrator/prompt/user/push/QA)별로 묶었다. 기획·문서·인프라·테스트·QA 실행처럼
> 레이어에 속하지 않는 항목은 맨 아래 "레이어 외 활동"에 모았다. 내용 삭제 없음 — 위치만 재배열.

### Entity
- [x] [bible] 성경 DB 스키마 (Flyway V1) + 66권 메타데이터 시드 (V2) — PostgreSQL에서 적용 검증 완료
- [x] [bible] 개역한글 본문 TSV 임포터 (`bible-import` 프로파일) — 소스 확정 즉시 적재 가능
- [x] [bible] 개역한글 성경 텍스트 확보 → 임포터로 적재 완료 (D-016). 대한성서공회 공식
      성경읽기 페이지(bskorea.or.kr, HAN 판) 스크래핑(`BibleTextScraper`, Jsoup) → TSV →
      기존 `BibleTextImporter`로 31,102절 적재. 각주·소제목 제거, 합쳐진 절 표기("18-19")는
      각 절 번호에 동일 본문 매핑. DB 검증: 총 절 수·책별 최대 장 번호 전부 일치, 샘플 구절
      (창1:1/시23:1/욘1:1/요3:16) 본문 육안 확인 완료. 테스트 3건 추가(`BibleTextScraperTest`)
- [x] [user] User — 소셜 로그인 사용자 정보 저장 (2026-07-16, `com.malssumbeot.user`): `User`(provider·providerId·
      email·nickname·타임스탬프, (provider,providerId) 유니크) + `AuthProvider`(GOOGLE/KAKAO/APPLE) + Flyway `V3__users.sql`.
      신앙 설문 필드·대화 이력은 보류(설문 설계/데이터 보관 정책 대기). 실제 PG 부팅으로 Flyway·validate 통과 확인
- [ ] [crisis] CrisisSessionStore 인메모리 → Redis/DB 이전 검토 (다중 서버 대비)

### Repository
- [x] [user] UserRepository (2026-07-16): `findByProviderAndProviderId` — 소셜 콜백에서 기존/신규 판단용. @DataJpaTest 2건
- [ ] [crisis] CrisisSessionStore 영속화용 Repository (위 Entity 항목과 동일 작업 — Redis/DB 이전 검토)

### DTO
- [x] [chat] 채팅 요청/응답 DTO 설계 (2026-07-16, `com.malssumbeot.api`): `ChatRequest`(sessionId·message
      @NotBlank), `ChatResponse`(ChatReply→API, passages를 구조화 Verse 리스트로 노출, D-003 유지)
- [ ] [auth] 로그인 요청/응답 DTO 설계

### Service
- [x] [bible] BibleVerseService: 구절 주소 파싱(풀네임/약어/범위/장절 표기) → DB 원문 조회 +
      존재 검증(없는 구절 `VerseNotFoundException` 거부, T8 환각 방지 기반) — 단위 테스트 20건 통과
- [x] [bible] 구절 검증: VerseReferenceScanner(응답 텍스트→주소 추출) + BibleVerseService 존재 검증
- [x] [bible] 구절 검증 보강 (theology-checker 2회차 WARN): VerseReferenceScanner에 장 단위 인용
      ("시편 23편", "눅 15장") 패턴 추가 + BibleVerseService 장 존재 검증(chapterCount 기반),
      존재하지 않는 장은 기존 환각 처리와 동일하게 재생성·제거. 모델이 성경 주소와 함께 생성한
      본문·풀이는 검증 여부와 무관하게 제거하고, DB 원문 `VersePassage`만 별도 전달 (D-017).
      위기 응답은 고정 연락처 안내로 결정론 처리, 영어 장절 표기도 환각 후보로 감지.
      테스트 추가 후 전체 76건 통과
- [x] [crisis] CrisisFilter 구현 (`com.malssumbeot.crisis`) — 결정론적 패턴 감지(직접/간접/학대,
      공백 변형 대응) + 세션 단위 sticky 위기 상태(기본 30분, D-012). REST API 작업 시
      인터셉터로 배선 예정. 패턴 목록은 사람 검토 대기 (아래 "사람 확인 필요")
- [x] [orchestrator] 의도 분류기 6분류 (상담/기도문/지식QA/일상대화/범위밖/위기) — 경량 모델
      `claude-haiku-4-5` (D-010)
- [x] [orchestrator] 모델 라우팅 (`ModelRouter`, D-013): 신앙(상담·QA·기도문·위기)=`claude-sonnet-4-6`,
      일상·범위밖=`claude-haiku-4-5`
- [x] [orchestrator] ChatOrchestrator 파이프라인 조립: 위기 감지 → 의도 분류 → 프롬프트 분기 →
      모델 라우팅 → 응답 생성 → 구절 스캔·DB 검증 → 환각 시 1회 재생성. 위기 API 장애 시
      결정론적 폴백(109 안내)
- [x] [prompt] 시스템 프롬프트 7종 리소스 탑재 (`resources/prompts/`): 마스터/상담/기도문/지식QA/위기는
      PRD §5 원문 그대로, 일상대화·범위밖은 신규 초안(사람 승인 대기). PromptRepository·PromptAssembler
- [ ] [push] FCM/APNs 푸시 발송 연동 (오늘의 말씀 알림 등)
- [ ] [QA] QA 러너: T1~T8 자동 실행 → theology-checker 판정 → 리포트 저장
      (ChatOrchestrator를 입력으로, 신학 검사 기준으로 자동 판정)

### Controller
- [x] [chat] 채팅 REST API 엔드포인트 (2026-07-16): `ChatController` `POST /api/chat` → `ChatOrchestrator.handle`.
      단일 진입점이 위기 우회 불가를 보장(handle이 위기 우선). @WebMvcTest 슬라이스(매핑·검증) +
      @SpringBootTest 통합테스트(위기 E2E, 모델 미호출)
- [ ] [auth] 인증 엔드포인트 (카카오/구글/Apple 소셜 로그인)

### Filter / Interceptor
- [~] [crisis] 위기 우회 불가 배선: 2026-07-16 인터셉터 대신 **단일 진입점**으로 보장(민규 결정 — 인터셉터는
      위기 응답 생성을 복제하거나 바디 재파싱이 필요해 이중감지·복잡. 엔드포인트가 늘면 그때 도입).
      통합테스트 `ChatApiIntegrationTest`로 위기 메시지가 HTTP 경로에서 모델 없이 위기 프로토콜로 가는 것 검증

### 레이어 외 활동 (기획 / 문서 / 인프라 / 테스트 / QA 실행)
- [x] 제품기획서 v1 작성 (`docs/PRD.md`)
- [x] CLAUDE.md (프로젝트 헌법) 작성
- [x] 신학 검사 에이전트 프롬프트 작성 (`agents/theology-checker.md`)
- [x] 메모리 파일 초기화 (PROGRESS.md, DECISIONS.md)
- [x] Spring Boot 3.5.15 프로젝트 스캐폴딩 (`backend/`, Maven, Java 17 타겟) — D-009
- [x] 로컬 개발용 PostgreSQL docker-compose (호스트 포트 55432 — 로컬 PG 18과 충돌 회피)
- [x] Anthropic Java SDK(2.40.1) 연동 — `AnthropicClient` 빈, API 키는 env var(미설정 시 부팅은 가능)
- [x] 신학 검사 에이전트 실행(분류 프롬프트 검토) → **FAIL(critical)**: 위기 감지 후 형식 위반 시
      상담으로 강등되는 파싱 경로 지적 → "위기" 부분 일치 우선 파싱으로 코드 수정 완료 (D-011).
      프롬프트 본문 수정안 3건은 사람 승인 대기 (아래 "사람 확인 필요")
- [x] 학습 자료 `docs/study/` 01~06 작성 (셋업/스캐폴딩/DB/환각방지/Claude연동/위기감지) —
      매 세션 갱신 루틴화 (CLAUDE.md 루틴 5번, 민규 지시)
- [x] 테스트 45건 통과
- [x] 신학 검사(theology-checker) 2회 실행:
      · 1회차 — 분류 프롬프트 critical(위기 강등) → 코드 수정(D-011)
      · 2회차 — 신규 프롬프트+파이프라인. **C1 FAIL**: 재생성 후에도 환각 주소가 본문에 남으면
        '그대로 전송'하는 것이 T8 '거부' 기준 위반 → **코드 수정(D-014)**: 미검증 주소 포함 문장을
        본문에서 제거(stripSentencesContaining), 비면 폴백 문구. 환각 주소가 사용자 화면에 닿지 않음
- [x] 테스트 63건 통과
- [x] 테스트 73건 통과
- [x] 테스트 74건 통과
- [x] 테스트 76건 통과
- [x] 테스트 81건 통과 (D-020 위기 sticky 단계 하강)
- [x] 테스트 85건 통과 (Phase 1 API 계층: 컨트롤러 슬라이스 3 + 위기 E2E 통합 1)

## 진행 중

- [ ] (없음 — Phase 1 완료. 다음: Phase 2 인증 & 사용자 — User 엔티티/소셜 로그인/대화 이력)

## 모바일 다음 작업 (React Native + Expo)
1. [ ] Expo 프로젝트 스캐폴딩, 채팅 UI (메시지 리스트, 성경 구절 인용 블록 구분 렌더링)
2. [ ] 로그인 플로우 + 대화 이력 동기화
3. [ ] 오늘의 말씀 푸시 알림 (수신 동의 기반)
4. [ ] 스토어 제출 준비: 개인정보처리방침, AI 생성 콘텐츠 고지, 신고 버튼
   (앱 내 신고 → theology-checker 1차 분류 큐 연동)

## 사람 확인 필요 (블로킹)

> 반영 현황(2026-07-15, D-020): **Phase 0의 승인 게이트 대부분 해소** — 신규 프롬프트 5건, 환각 폴백
> 문구, crisis-patterns.txt(수정 없음 확정), sticky 정책(→ 단계 하강 D-020) 전부 민규 승인·반영·테스트
> 81건 통과. **여전히 대기(전문가/법률 검토)**: (1) 위기 MID/LOW 신규 문구 + 시간 하강 설계의 프로덕션
> 배포 전 정신건강 전문가 검토(인라인 신학 검사 조건부 PASS, severity high), (2) 학대 전용 기관 번호·
> 미성년자 진술 데이터 보관 정책(법률 검토), (3) 성명표시권 표기, (4) 패키지 명칭(webhook→api).

- [ ] 성명표시권 표기 문구 확정 (D-016): 개역한글은 재산권은 만료됐지만 성명표시권(저작자 표시)은
  영구 권리라 RN 앱 어딘가(설정/정보 화면)에 "성경전서 개역한글판, 대한성서공회" 표기가 필요함.
  정확한 문구·위치는 RN 앱 작업 시 확정 (모바일 다음 작업 #4 스토어 제출 준비와 함께 처리 제안)
- [x] (2026-07-16 완료) CLAUDE.md 패키지 컨벤션 `webhook`→`api` 갱신. 컨트롤러·DTO는 `com.malssumbeot.api`
- [x] (2026-07-14 완료) ANTHROPIC_API_KEY 발급·설정 + curl 스모크 테스트 성공.
  앱 통한 분류기 실호출 검증은 Phase 1 앱 실행 시
- [x] (2026-07-15 완료) **의도 분류 프롬프트 승인** (`backend/src/main/resources/prompts/intent-classifier.txt`):
  신규 작성 프롬프트이므로 검토 요청. theology-checker(2026-06-12, critical FAIL)의
  프롬프트 수정안 3건도 함께 승인 필요 — 승인 전에는 반영하지 않음:
  1. 위기 범주에 제3자 위기 신호 추가 ("친구가 죽고 싶대요", 타인 위해 위협)
  2. 위기 범주에 혼합 예시 추가 ("다른 요청 속에 끼어든 위기 표현 포함")
  3. 기도문 범주에 경계 문구 추가 ("질병의 치유 여부를 묻는 질문은 기도문이 아님" —
     T4가 기도문 분기로 끌려가 치유 확언 위험 방지)
- [x] (2026-07-15 완료) **위기 감지 패턴 목록 검토** (`backend/src/main/resources/crisis/crisis-patterns.txt`):
  민규 검토 결과 추가·삭제 없음(수정 없이 확정). sticky는 30분 이분법 → 단계 하강(90분, D-020)으로 변경.
  후속: 애매 신호 LLM 2차 판정(전문가 검토), sticky 카테고리 보존
- [x] (2026-07-15 완료·반영, D-020) **신규 프롬프트 5건 승인** — 아래 전부 승인·반영:
  · 일상대화/범위밖 모드 프롬프트 본문 (`daily-chat.txt`, `out-of-scope.txt`) 자체
  · 두 모드에 위기 escape hatch 1줄 내장 ("위기 신호 시 즉시 위기 프로토콜")
  · 일상대화에 T2 회복 규칙 추가 (교회 불출석 고백을 정죄 없이 환대, 눅 15장 태도)
  · 위기 폴백 문구(`CRISIS_FALLBACK_TEXT`) "저도 곁에 있겠습니다" 표현 + 학대 카테고리
    별도 기관(1366 등) 분기 검토
  · 기도문 프롬프트에 T7(로또 기도) 경계 문구 ("기복적·도구적 요청 부드럽게 거절")
- [x] (2026-07-15 승인) **환각 폴백 문구 검토** (`HALLUCINATION_FALLBACK_TEXT`, ChatOrchestrator):
  어조·내용 민규 승인.
- [ ] **위기 MID/LOW 신규 문구 + 시간 하강 설계(D-020) 전문가 검토**: 위기 강도가 MID/LOW로 내려온
  구간의 `CRISIS_MID_TEXT`/`CRISIS_LOW_TEXT`(연락처 미반복, 곁 지키는 톤)와 시간 기반 하강이 실제 위기
  사용자를 방치하지 않는지 정신건강 전문가 검토 필요. 인라인 신학 검사 조건부 PASS(severity high) —
  프로덕션 배포 전 검토 필수. 후속 (a) LLM 2차 판정과 함께 처리 권장
- [ ] **미성년자 학대 응답 문구 + 진술 데이터 보관 정책 (법률 검토 필요)**: 학대 신호 시 응답은
  공감 + 전문 기관(신고·상담 번호) 연결로 하되, "우리 대화 기록이 증거가 된다"는 약속은 하지 않는다
  (지킬 수 없는 약속·오히려 위험). 미성년자 학대 진술을 우리가 저장·보관·제공하는 범위는
  개인정보처리방침·법률 검토 대상 — 스토어 제출 준비의 개인정보처리방침과 함께 확정
  (2026-07-14 민규 논의, 위기 응답 완화 결정 A와 함께 정리)
  · 코드 반영 현황(2026-07-14): 위기 응답을 자살자해/학대로 분기 완료
  (`CRISIS_SELF_HARM_TEXT`/`CRISIS_DEFAULT_TEXT`). 학대·카테고리 불명확은 '믿을 만한 사람' 권유
  제거 + 112 안내한 **안전 잠정본**. 학대 전용 문구·기관 번호(1366·아동보호전문기관 등) 확정은 이 항목에서.
- [ ] Apple Developer($99/년) / Google Play($25) 개발자 계정 등록
- [ ] 소셜 로그인용 카카오/구글/Apple 개발자 콘솔 앱 등록
- [x] (2026-07-16 확정, D-021) 서비스명: 앱=엠마오, 챗봇=바나바. 내부 패키지 malssumbeot 유지.
  챗봇 자기지칭 "바나바"를 프롬프트에 반영하는 것은 사람 승인 항목이라 프롬프트 작업 시 처리
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
| 2026-06-12 | CrisisFilter(패턴 감지 + sticky 30분) 구현, 학습 자료 docs/study/ 01~06 작성 + 세션 루틴화. 테스트 45건 통과 | 완료 |
| 2026-06-12 | 프롬프트 7종 탑재 + ModelRouter + ChatOrchestrator 파이프라인(구절 검증·환각 sanitize·위기 폴백). 신학 검사 2회차 C1 FAIL → 코드 수정(D-014). 테스트 63건 통과. 프롬프트 5건 승인 대기 | 완료 |
| 2026-07-02 | 개역한글 텍스트 소스 확정(대한성서공회 공식 성경읽기 페이지, D-016) + `BibleTextScraper`(Jsoup) 신규 작성 → TSV 생성 → 기존 임포터로 31,102절 DB 적재, 검증 완료. `BibleBookCatalog`에 영문 코드 해석 추가. 브랜치 `feature/bible-text-import` | 완료 |
| 2026-07-12 | 장 단위 인용 검증 보강: `시편 23편`·`눅 15장` 스캔 및 chapterCount 검증 추가. 존재하지 않는 장은 재생성·제거 경로로 처리. 모델이 성경 주소와 함께 생성한 본문·풀이는 모두 제거하고 DB 원문만 별도 전달(D-017). 위기는 고정 연락처 안내로 결정론 처리, 영어 장절도 환각 후보로 감지. 테스트 76건 통과. 브랜치 `feature/verse-reference-validation` | 완료 |
| 2026-07-15 | **Phase 0 완료**: 프롬프트 5건 승인·반영(daily-chat/out-of-scope 본문, 위기 escape hatch, T2 회복규칙, T7 로또경계), 환각 폴백 문구 승인, crisis-patterns 수정없음 확정. 위기 sticky를 단계 하강(HIGH→MID→LOW→해제, 90분)으로 재설계 + MID/LOW 문구 2종(D-020, Model 2). 성경 DB 적재 확인(31,102절). 인라인 신학 검사: 프롬프트 PASS, 위기 경로 조건부 PASS(severity high, 전문가 검토 권장). 테스트 81건 통과. 브랜치 `feature/crisis-response-branching` | 완료 |
| 2026-07-16 | **Phase 1 완료**: 채팅 HTTP 계층. `com.malssumbeot.api` 신규 — `ChatRequest`/`ChatResponse` DTO, `ChatController`(`POST /api/chat`). sessionId 바디 필드, 위기 우회 불가는 단일 진입점으로 보장(인터셉터 후속, 민규 결정). CLAUDE.md 컨벤션 webhook→api. @WebMvcTest 3 + 위기 E2E 통합테스트 1 추가, 테스트 85건 통과 | 완료 |
