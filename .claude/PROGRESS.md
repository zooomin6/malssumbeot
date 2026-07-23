# PROGRESS.md — 말씀벗 진행 상황

> 모든 에이전트 세션은 이 파일을 먼저 읽고, 작업 후 갱신한다.
> 마지막 갱신: 2026-07-22 (독립 코드 리뷰 후속 조치: 성경 근거 파이프라인 재설계(D-025),
> 위기 sticky 재설계(D-026), P1/P2 코드 수정 6건, prayer.txt 번영신학 표현 수정. 테스트 108건)

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
- [x] [auth] 로그인 요청/응답 DTO 설계 (2026-07-20, `com.malssumbeot.auth`): `LoginRequest`(token @NotBlank),
      `LoginResponse`(accessToken=자체 JWT, provider, nickname, email — 동의 범위 따라 null 가능)

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
- [x] [bible/orchestrator/prompt] **성경 근거 파이프라인 재설계 (2026-07-22, D-025 — D-017 대체)**:
      독립 코드 리뷰에서 `PromptAssembler`의 grounding 인자가 실제로는 항상 빈 리스트로 호출되어
      모델이 DB 원문을 한 번도 보지 못한 채 생성하고 있었다는 것과(P0-1), 주소 없는 신앙적 주장은
      검증 자체를 우회한다는 것(P0-2), D-017의 "주소 있으면 전체 텍스트 차단"이 검증 성공 시에도
      모델의 공감 코멘트를 통째로 지우는 부작용(P0-3)이 확인됨. 2단계 grounded 생성으로 교체:
      1단계(신규 `verse-address-proposal.txt`, 경량 모델)가 관련 구절 주소만 제안 → DB 검증 →
      검증된 원문만 2단계 `<bible_verses>`에 실어 최종 응답 생성. 검증된 구절이 없으면(상담/기도문/
      지식QA 한정) "인용하지 말라" 안내를 대신 삽입(P0-2 대응). 2단계 출력에 미검증 주소가
      스캔되면 그 주소만 제거하고 코멘트는 보존(D-017 폐기). `Intent.requiresGrounding()` 신규.
      기존 "환각 시 1회 재생성" 루프 제거. 테스트 재작성 후 전체 108건 통과
- [x] [crisis] CrisisFilter 구현 (`com.malssumbeot.crisis`) — 결정론적 패턴 감지(직접/간접/학대,
      공백 변형 대응) + 세션 단위 sticky 위기 상태. REST API 작업 시 인터셉터로 배선 예정.
      패턴 목록은 사람 검토 대기 (아래 "사람 확인 필요")
- [x] [crisis] **위기 sticky 재설계 (2026-07-22, D-026 — D-020의 시간 하강 폐기)**: 실제 대화
      시뮬레이션 중 시간 기반 하강(HIGH→MID→LOW→NONE, 최장 90분)이 사용자가 완전히 다른 화제로
      넘어가려 해도 메시지 내용과 무관하게 최장 90분간 위기 문구를 반환한다는 문제 확인. 아울러
      제품 페르소나(PRD.md 2.2)가 위기 상황 사용자를 상정하지 않는다는 논의도 있었음 — 위기 대응을
      "핵심 기능"이 아닌 "최소 안전망"으로 재포지셔닝(단, PRD 1.4/CLAUDE.md 절대원칙 5의 안전망
      자체는 유지). 새 메커니즘: 위기 신호 → 문구 응답 → **바로 다음 메시지 1회만** sticky 확인
      → 그 메시지에 위기 패턴 없으면 즉시 새 요청대로 정상 처리. `CrisisLevel`·`CRISIS_MID_TEXT`/
      `CRISIS_LOW_TEXT`·`sticky-duration` 설정 전부 제거. 트레이드오프(감수하기로 함): 패턴에
      안 걸리는 말로 화제를 돌리면 실제로 힘든 상태여도 즉시 위기 모드를 빠져나갈 수 있음
- [x] [orchestrator] 의도 분류기 6분류 (상담/기도문/지식QA/일상대화/범위밖/위기) — 경량 모델
      `claude-haiku-4-5` (D-010)
- [x] [orchestrator] 모델 라우팅 (`ModelRouter`, D-013): 신앙(상담·QA·기도문·위기)=`claude-sonnet-4-6`,
      일상·범위밖=`claude-haiku-4-5`
- [x] [orchestrator] ChatOrchestrator 파이프라인 조립 (2026-07-22 D-025로 갱신): 위기 감지 → 의도
      분류 → (신앙 인텐트만) 1단계 구절 주소 제안·DB 검증 → 프롬프트 조립(검증된 원문 포함) →
      2단계 응답 생성 → 잔여 미검증 구절만 제거. 위기 응답은 여전히 모델 호출 없이 결정론적
- [x] [prompt] 시스템 프롬프트 8종 리소스 탑재 (`resources/prompts/`): 마스터/상담/기도문/지식QA/위기는
      PRD §5 원문 그대로, 일상대화·범위밖은 승인된 초안, `verse-address-proposal.txt`(D-025 1단계
      전용, 2026-07-22 신규)는 인텐트 무관. PromptRepository·PromptAssembler(P0-2 안내 삽입 포함)
- [x] [api/auth/bible] **독립 코드 리뷰 후속 P1/P2 코드 수정 6건 (2026-07-22)**:
      1. `ChatResponse`에서 `unverifiedReferences` 공개 API 노출 제거 (서버 로그로만 추적)
      2. 사용자별 채팅 요청 한도(`ChatRateLimiter`, 시간당 30건) + 메시지 길이 상한(4000자) 추가
      3. Kakao/GoogleTokenVerifier: 제공자 서버 장애(5xx·네트워크)와 실제 무효 토큰(4xx)을
         구분해 전자는 `SocialProviderUnavailableException`(503)으로 분리
      4. `BibleTextImporter.run()`을 `@Transactional`로 감싸 중간 실패 시 전체 롤백
      5. `docs/PRD.md` 아키텍처 다이어그램의 대화 이력 표기(D-024 미반영)·성경 근거 파이프라인
         설명(임베딩 검색 언급, 실제 미구현)을 실제 구현(D-025)에 맞게 갱신
      6. `VerseReferenceScanner`의 한글 가짜 책이름(예: "에녹서 3:16") 검증 우회는 **수정 시도 후
         되돌림** — 한글은 정규식만으로 "가짜 책이름"과 "시각·점수 표현"(예: "오후 3:30")을
         구분할 수 없고, 책이름 해석 성공 여부가 그 구분 필터를 겸하고 있어 블랜킷 수정 시 기존
         정상 케이스가 회귀함. 근본 해결책 없이 알려진 한계로 남김 (아래 "향후 논의" 참고)
- [ ] [push] FCM/APNs 푸시 발송 연동 (오늘의 말씀 알림 등)
- [ ] [QA] QA 러너: T1~T8 자동 실행 → theology-checker 판정 → 리포트 저장
      (ChatOrchestrator를 입력으로, 신학 검사 기준으로 자동 판정)

### Controller
- [x] [chat] 채팅 REST API 엔드포인트 (2026-07-16): `ChatController` `POST /api/chat` → `ChatOrchestrator.handle`.
      단일 진입점이 위기 우회 불가를 보장(handle이 위기 우선). @WebMvcTest 슬라이스(매핑·검증) +
      @SpringBootTest 통합테스트(위기 E2E, 모델 미호출)
- [x] [auth] 인증 엔드포인트 (2026-07-20, D-022 방식 A): `AuthController` `POST /api/auth/{provider}`(google|kakao)
      → `AuthService`(제공자 토큰 검증 → User upsert → JWT 발급). 제공자별 `SocialTokenVerifier`(GoogleTokenVerifier=
      구글 라이브러리로 ID토큰 검증, KakaoTokenVerifier=사용자정보 API 호출), `JwtService`(jjwt). 애플은 미구현(400).
      `/api/chat`에 JWT 인증 배선 완료(2026-07-20, D-023 — 아래 Filter/Interceptor 참조). 테스트 98건 통과

### Filter / Interceptor
- [~] [crisis] 위기 우회 불가 배선: 2026-07-16 인터셉터 대신 **단일 진입점**으로 보장(민규 결정 — 인터셉터는
      위기 응답 생성을 복제하거나 바디 재파싱이 필요해 이중감지·복잡. 엔드포인트가 늘면 그때 도입).
      통합테스트 `ChatApiIntegrationTest`로 위기 메시지가 HTTP 경로에서 모델 없이 위기 프로토콜로 가는 것 검증
- [x] [auth] JWT 인증 배선 (2026-07-20, D-023): `JwtAuthInterceptor`(HandlerInterceptor) + `WebConfig`가
      `/api/**` 보호·`/api/auth/**` 제외. 헤더 `Authorization: Bearer`의 JWT를 `JwtService.parse`로 검증,
      실패 시 `UnauthenticatedException`(401), 성공 시 userId를 request 속성(`authUserId`)에 실음. 신원 모델
      A(공존): sessionId·위기 로직·DTO 무변경. 슬라이스 테스트 4건(무토큰/형식오류/깨진토큰→401, 유효→200,
      401 시 오케스트레이터 미호출) + 기존 슬라이스에 `@Import(JwtService.class)` 보정. 테스트 103건 통과

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
- [x] 테스트 108건 통과 (2026-07-22: D-025 성경 근거 파이프라인 재설계 + D-026 위기 sticky
      재설계 + P1/P2 코드 수정 6건 반영 후 전체 회귀)

## 진행 중

- [ ] (없음 — Phase 2 소셜 로그인 + `/api/chat` JWT 보호(D-023) 완료. **대화 이력 저장은 MVP 범위 밖으로
  결정(D-024)**. 다음 후보: CrisisSessionStore 영속화(서버 1대엔 불요), 애플 로그인(콘솔+구현),
  또는 **Phase 3 모바일**(권장))

## 모바일 다음 작업 (React Native + Expo)
1. [ ] Expo 프로젝트 스캐폴딩, 채팅 UI (메시지 리스트, 성경 구절 인용 블록 구분 렌더링)
2. [ ] 로그인 플로우 + 대화 이력 동기화
3. [ ] 오늘의 말씀 푸시 알림 (수신 동의 기반)
4. [ ] 스토어 제출 준비: 개인정보처리방침, AI 생성 콘텐츠 고지, 신고 버튼
   (앱 내 신고 → theology-checker 1차 분류 큐 연동)

## 사람 확인 필요 (블로킹)

> 반영 현황(2026-07-15, D-020): **Phase 0의 승인 게이트 대부분 해소** — 신규 프롬프트 5건, 환각 폴백
> 문구, crisis-patterns.txt(수정 없음 확정), sticky 정책(→ 단계 하강 D-020) 전부 민규 승인·반영·테스트
> 81건 통과. **여전히 대기(전문가/법률 검토)**: (1) [2026-07-22 해소 — 아래 참고] (2) 학대 전용
> 기관 번호·미성년자 진술 데이터 보관 정책(법률 검토), (3) 성명표시권 표기, (4) 패키지 명칭(webhook→api).
>
> 반영 현황(2026-07-22): D-020의 시간 하강 자체를 폐기하고 1회성 sticky로 재설계(D-026, 아래
> 참고) — 원래 대기 중이던 "MID/LOW 문구 + 시간 하강 전문가 검토" 항목은 그 메커니즘 자체가
> 없어져 **대상 소멸**. prayer.txt 번영신학 표현도 신학 검사 4차 반복 끝에 수정·통과(아래 참고).

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
  어조·내용 민규 승인. (2026-07-22: D-025로 파이프라인 자체가 바뀌며 이 상수와 관련 로직은
  제거됨 — 전체 텍스트 대체가 아니라 미검증 구절만 부분 제거하는 방식으로 대체, DECISIONS.md D-025)
- [x] (2026-07-22 해소, D-026) ~~위기 MID/LOW 신규 문구 + 시간 하강 설계(D-020) 전문가 검토~~:
  D-020의 시간 기반 하강 메커니즘 자체를 폐기(1회성 sticky로 교체)하면서 `CRISIS_MID_TEXT`/
  `CRISIS_LOW_TEXT`와 이 검토 항목 모두 대상 소멸. 새 메커니즘의 트레이드오프(패턴에 안 걸리는
  말로 화제 전환 시 실제 위기여도 즉시 빠져나갈 수 있음)는 민규가 명시적으로 감수하기로 결정.
- [x] (2026-07-22 승인·반영) **prayer.txt 번영신학 표현 수정**: 18-20행 "기다리다보면 하나님은
  응답을 해주신다... 폭포수와같은 은혜"(무조건 보장) 제거. theology-checker 정적 검토를 4차
  반복(모세 광야→C1 끼워맞춤 FAIL, 한나→결과 등치 FAIL, **하박국으로 최종 확정**: "어느
  때까지니이까" 탄식 후 상황이 아닌 하나님의 신실하심을 붙들기로 한 구조라 특정 결과를
  약속하지 않음). 무당/타로/신점 비유는 사용자가 먼저 언급했을 때만 사용하도록 조건화(정죄
  톤 방지). 성경 이야기 인용은 이름·상황만 허용, 세부 재구성 금지(C1 우회 경로 차단)
- [ ] **미성년자 학대 응답 문구 + 진술 데이터 보관 정책 (법률 검토 필요)**: 학대 신호 시 응답은
  공감 + 전문 기관(신고·상담 번호) 연결로 하되, "우리 대화 기록이 증거가 된다"는 약속은 하지 않는다
  (지킬 수 없는 약속·오히려 위험). 미성년자 학대 진술을 우리가 저장·보관·제공하는 범위는
  개인정보처리방침·법률 검토 대상 — 스토어 제출 준비의 개인정보처리방침과 함께 확정
  (2026-07-14 민규 논의, 위기 응답 완화 결정 A와 함께 정리)
  · 코드 반영 현황(2026-07-14): 위기 응답을 자살자해/학대로 분기 완료
  (`CRISIS_SELF_HARM_TEXT`/`CRISIS_DEFAULT_TEXT`). 학대·카테고리 불명확은 '믿을 만한 사람' 권유
  제거 + 112 안내한 **안전 잠정본**. 학대 전용 문구·기관 번호(1366·아동보호전문기관 등) 확정은 이 항목에서.
  · **저장 정책 확정(2026-07-20, D-024)**: MVP는 **대화 이력을 DB에 저장하지 않음**(세션 내 개인화만).
    → 진술 데이터 보관 정책 이슈 자체가 MVP에선 소멸. 위기 대응(CrisisSessionStore)은 저장과 무관하게
    그대로 작동(2026-07-22, D-026로 시간 하강→1회성 sticky). 향후 대화 이력 추가 시 저장 범위·
    미성년자/나이 정책·14세 미만 계정 동의를 재검토(2026-07-22 세션에서 재확인: 저장이 기술적으로
    어려운 게 아니라, 민감정보 등급·미성년자 보호·사용자 권리 보장 의무가 새로 생기는 게 이유임).
- [ ] Apple Developer($99/년) / Google Play($25) 개발자 계정 등록
- [~] 소셜 로그인용 카카오/구글/Apple 개발자 콘솔 앱 등록 — 카카오(7/20)·구글 완료. 애플만 남음(로그인 미구현, iOS 착수 시)
- [x] (2026-07-16 확정, D-021) 서비스명: 앱=엠마오, 챗봇=바나바. 내부 패키지 malssumbeot 유지.
  챗봇 자기지칭 "바나바"를 프롬프트에 반영하는 것은 사람 승인 항목이라 프롬프트 작업 시 처리
- [ ] RN 담당 협업자 확정 (친구 협업 vs 직접 개발)
- [ ] **개인 맞춤 온보딩 + 7일 여정 기획(`docs/features/onboarding-7day-journey.md`) 열린 질문** (2026-07-18 민규
  제안, 경쟁 앱 벤치마킹). 파일에 원안 보존 + 조정 반영. 착수 전 확정 필요한 항목:
  1. **교단별 "해석" 반영 범위** — 개신교 교단(장로교·감리교·순복음 등)만 남김(가톨릭·성공회 제거,
     번역본 설정 제거=개역한글 고정). 그러나 "교단에 맞는 해석"을 어디까지 허용할지 미정: 표현·용어·
     친숙도 조절까지만인가, 교리 해석까지인가. 후자는 절대원칙 3(교단 중립·특정 교단 정답 하드코딩 금지) 위반 위험
  2. **온보딩 답변·감정 기록 저장 여부** — 민규 잠정 "굳이 저장 안 함". MVP는 세션 내 개인화만.
     영속 저장 시 대화 이력 저장 정책(법률 검토, Phase 2)과 함께 확정
  3. **신앙 친숙도별 답변 방식** = 프롬프트 변경(사람 승인), **결제 정책**(§10.3) = 결제 설정(사람 승인)
- [ ] **위기 문구 반복이 오히려 생각을 상기시킬 수 있다는 우려 (2026-07-22 민규 제기, 별도 논의 필요)**:
  sticky(D-026)가 재적용될 때마다 같은 위기 문구(`CRISIS_SELF_HARM_TEXT` 등)가 그대로 반복되는데,
  실제로는 위기가 아니었던 사용자에게도 반복 노출 자체가 그 생각을 더 상기시킬 수 있다는 지적.
  문구 반복 자체를 줄이거나(2회차부터 표현을 다르게) 톤을 조절하는 방안은 위기 프로토콜
  내용 수정(사람 승인 필요 항목)이라 오늘 범위에서 다루지 않고 별도 세션에서 논의.

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
| 2026-07-20 | **Phase 2 소셜 로그인**: 방식 A(토큰 검증형, D-022) 결정. `com.malssumbeot.auth` 신규 — `POST /api/auth/{provider}`(google/kakao), 제공자별 TokenVerifier(구글 ID토큰·카카오 사용자정보 API) + JwtService(jjwt) + AuthService(User upsert). 카카오 개발자 콘솔 앱 등록(엠마오, 이메일은 비즈앱 전까지 미수집). 의존성 jjwt·google-api-client 추가. 테스트 98건 통과. 브랜치 `feature/oauth-login` | 완료 |
| 2026-07-20 | **대화 이력 저장 = MVP 범위 밖 결정(D-024)**: "돕기(실시간)"와 "저장(DB)"을 분리 — 위기 대응은 CrisisSessionStore(타임스탬프만, 시간 하강)가 담당하므로 대화 이력 DB 저장은 불필요. MVP는 세션 내 개인화만. 앱 전용 출시라 기기 동기화 후순위. 민감정보·미성년자·개인정보처리방침 리스크 소멸. DECISIONS/ROADMAP 갱신 | 완료 |
| 2026-07-20 | **`/api/chat` JWT 인증 배선(D-023)**: `JwtAuthInterceptor`(HandlerInterceptor)+`WebConfig`로 `/api/**` 보호·`/api/auth/**` 제외. `UnauthenticatedException`(401). 신원 모델 A(sessionId 공존, 위기 로직 무변경). Spring Security 미도입(경량). 슬라이스 테스트 4건 + 기존 @WebMvcTest에 `@Import(JwtService.class)` 보정. 학습자료 11장 갱신. 테스트 103건 통과 | 완료 |
| 2026-07-16 | **Phase 1 완료**: 채팅 HTTP 계층. `com.malssumbeot.api` 신규 — `ChatRequest`/`ChatResponse` DTO, `ChatController`(`POST /api/chat`). sessionId 바디 필드, 위기 우회 불가는 단일 진입점으로 보장(인터셉터 후속, 민규 결정). CLAUDE.md 컨벤션 webhook→api. @WebMvcTest 3 + 위기 E2E 통합테스트 1 추가, 테스트 85건 통과 | 완료 |
| 2026-07-22 | **독립 코드 리뷰 후속 조치(별도 세션 리뷰 결과 반영)**: (1) **성경 근거 파이프라인 재설계(D-025)** — grounding 미배선(P0-1)·주소없는 주장 미검증(P0-2)·D-017 전체삭제 부작용(P0-3) 해소. 2단계 grounded 생성(1단계 경량 모델 주소 제안 → DB 검증 → 2단계 원문 포함 생성), 신규 `verse-address-proposal.txt`. (2) **P1/P2 코드 수정 6건**: unverifiedReferences API 노출 제거, rate limit(시간당 30건)+메시지 길이 제한, Kakao/Google 5xx·4xx 구분, BibleTextImporter 트랜잭션, docs/PRD.md 갱신, 한글 가짜 책이름 우회 수정은 회귀로 보류. (3) **위기 sticky 재설계(D-026)** — 시간 기반 하강(D-020) 폐기, 화제 전환 시 즉시 새 요청 처리하는 1회성 방식으로 교체(민규 결정 — 제품 페르소나가 위기 상황자를 상정하지 않는다는 논의 포함, 안전망 자체는 유지). (4) **prayer.txt 번영신학 표현 수정** — theology-checker 정적 검토 4회 반복(모세→한나→하박국) 끝에 확정. 테스트 108건 통과. 위기 문구 반복의 심리적 영향은 별도 논의로 이월 | 완료 |
