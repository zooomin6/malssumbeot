# ROADMAP.md — 말씀벗 실행 로드맵

> 순차 체크리스트. 위에서부터 하나씩 `[x]`로 지워가며 진행한다.
> 상세 진행 기록은 `.claude/PROGRESS.md`, 결정 근거는 `.claude/DECISIONS.md` 참조.
> MVP 완료 기준(게이트)은 `.claude/CLAUDE.md`의 Definition of Done.

**범례**
`[민규]` 사람만 가능 (승인·계정·키) · `[코드]` 코드 작업 (민규+claude) ·
`[검증]` 동작 확인 · 🚩DoD Definition of Done 게이트 항목

**현재 상태 (2026-07-16, 실제 코드 기준)**
코어 파이프라인 + **채팅 HTTP 계층(`POST /api/chat`)** 완성, 단위·통합 테스트 85건. 사용자/로그인,
푸시, QA 자동러너, 모바일 앱은 아직 미구현.
**Phase 0 완료(2026-07-15)**: 프롬프트 5건 승인, 위기 sticky 단계 하강(D-020).
**Phase 1 완료(2026-07-16)**: ChatRequest/Response DTO, ChatController, sessionId 바디, 위기 우회불가
= 단일 진입점 보장(인터셉터 후속), `api` 패키지. 위기 우회불가는 통합테스트로 검증.
다음은 Phase 2(인증 & 사용자). MVP 목표는 RN 앱(D-002).
                                                         
---

## Phase 0 — 실제로 도는지 확인 + 승인 잠금 해제
> 지금껏 실제 API를 한 번도 안 불렀고, 프롬프트·위기패턴은 사람 승인 전이라 확정 불가.
> 여기부터 풀어야 뒤가 진짜로 진행된다.

- [x] [민규] `ANTHROPIC_API_KEY` 발급 → 환경변수 설정 (외부 키 = 사람 승인 항목) — 2026-07-14 완료. 창별 세션이라 새 터미널에선 재-export 필요
- [x] [민규] 의도 분류 프롬프트(`intent-classifier.txt`) 승인 — 2026-07-14 제3자·변형·분노제외·애도·T4·규칙3 반영, 테스트 통과
- [x] [민규] '사랑' 대전제 명시 (master.txt + CLAUDE.md 절대원칙, D-018)
- [x] [민규] 신규 프롬프트 5건 승인 (daily-chat/out-of-scope 본문, 위기 escape hatch, T2 회복규칙, T7 경계) — 2026-07-15 승인·반영 (D-020)
- [x] [민규] 위기 폴백 문구 — 완화 + 카테고리 세분화(자살자해/제3자/학대/불명확), 79건 통과 (D-018·D-019)
- [x] [민규] 환각 폴백 문구(`HALLUCINATION_FALLBACK_TEXT`) 검토 — 2026-07-15 승인 (D-020)
- [x] [민규] 위기 감지 패턴(`crisis-patterns.txt`) 전체 검토 + sticky — 2026-07-15 패턴 수정 없음 확정, sticky는 단계 하강(HIGH→MID→LOW→해제, 90분)으로 변경(D-020). 2단계(LLM 애매판정)·sticky 카테고리 보존은 후속
- [x] [검증] 실제 Claude 호출 스모크 테스트 1회 — 2026-07-14 curl로 claude-haiku-4-5 직접 호출 성공(결제 정상). 앱 통해 분류기 실호출 검증은 Phase 1 앱 실행 시
- [x] [검증] 성경 본문이 대상 DB에 적재돼 있는지 확인 — 2026-07-15 docker PG(55432) 기동, bible_verse 31,102절·66권·샘플(창1:1/시23:1/요3:16) 정상. 재적재 불필요

## Phase 1 — 백엔드 HTTP 계층 (두뇌에 문 달기) · M1 실질 마무리
> `ChatOrchestrator.handle()`을 밖에서 부를 수 있게 만든다. 지금 가장 큰 공백.

- [x] [코드] 채팅 요청/응답 DTO 설계 — 2026-07-16 `ChatRequest`(sessionId·message, @NotBlank) + `ChatResponse`(ChatReply→API, passages 구조화)
- [x] [코드] 채팅 REST API 컨트롤러 (`POST /api/chat`) → `ChatOrchestrator.handle` 연결 — 2026-07-16 `com.malssumbeot.api.ChatController`
- [x] [코드] `sessionId` 전달 방식 확정 — 2026-07-16 요청 바디 필드로 결정(인증 전 MVP, 민규 승인)
- [x] [코드] 위기 우회 불가 배선 — 2026-07-16 인터셉터 대신 **단일 진입점(ChatController→handle, handle이 위기 우선)**으로 보장(민규 결정). 통합테스트로 검증. 인터셉터는 엔드포인트 다수화 시 후속
- [x] [코드] 컨트롤러 패키지 명명 정리 (`webhook`→`api`) + CLAUDE.md 컨벤션 갱신 — 2026-07-16
- [~] [검증] E2E: HTTP 매핑·입력검증(@WebMvcTest) + 위기 우회불가 통합테스트(@SpringBootTest, 모델 미호출) 완료. **위기 3종·구절 인용·환각 거부 전수 E2E는 Phase 4 QA 러너**가 동일 엔드포인트로 수행(도메인 단위 테스트로는 이미 커버). 실기동 curl 스모크는 선택

## Phase 2 — 인증 & 사용자 (앱 로그인 기반)
> RN 앱 로그인과 대화 이력 동기화의 토대.

- [~] [민규] 카카오/구글/Apple 개발자 콘솔 앱 등록 (클라이언트 ID/시크릿) — 카카오(7/20)·구글 완료.
      애플만 남음(애플 로그인 미구현이라 iOS 착수 시 처리)
- [x] [코드] `User` 엔티티 + Flyway `V3` 마이그레이션 + `UserRepository` — 2026-07-16 완료 (`com.malssumbeot.user`,
      PG 부팅 validate 통과). 인증 신원만 담음.
      · 회원가입 설문 "신앙 시작 시기" 필드는 **보류** — 설문 UI 설계(Phase 3) 때 필드 확정 후 추가.
        기도문 개인화는 "깊이 우열"이 아니라 "익숙한 표현 수준 조절"로 프레이밍(대전제: 판단보다 경청)
      · 대화 이력 저장도 **보류** — 위기·학대 진술 데이터 보관 정책(법률 검토) 확정 후
- [x] [코드] 소셜 로그인(OAuth) 인증 엔드포인트 + 인증 DTO + 토큰 발급 — 2026-07-20 완료. 방식 A(토큰 검증형, D-022):
      `com.malssumbeot.auth` — `POST /api/auth/{provider}`(google|kakao), 제공자 토큰 검증(구글 ID토큰/카카오 사용자정보 API)
      → User upsert → 자체 JWT 발급. 테스트 98건 통과.
- [x] [코드] `/api/chat` JWT 인증 배선 — 2026-07-20 완료(D-023). `JwtAuthInterceptor`(HandlerInterceptor)+`WebConfig`가
      `/api/**` 보호·`/api/auth/**` 제외. 헤더 JWT를 `JwtService.parse`로 검증(실패=401 `UnauthenticatedException`).
      신원 모델 A(sessionId 공존, 위기 로직 무변경). Spring Security 미도입(경량). 테스트 103건 통과
- [—] [코드] 대화 이력 저장 설계 (엔티티 + 마이그레이션) — **MVP 범위 밖 결정(D-024)**. MVP는 세션 내
      개인화만, DB 저장 안 함(위기 대응은 CrisisSessionStore가 담당해 저장 불필요). 앱 전용 출시라 기기
      동기화 후순위. 향후 추가 시 저장 범위·미성년자/나이 정책·개인정보처리방침 재검토
- [ ] [코드] `CrisisSessionStore` 인메모리 → Redis/DB 이전 검토 (영속화·다중 서버)

## Phase 3 — 모바일 앱 (React Native + Expo)
> 실제 제품 표면. DoD의 E2E 대상.

- [ ] [민규] RN 담당자 확정 (친구 협업 vs 직접 개발)
- [ ] [코드] Expo 프로젝트 스캐폴딩
- [ ] [코드] 채팅 UI (메시지 리스트 + 성경 구절 인용 블록 구분 렌더링)
- [ ] [코드] 개인 맞춤 온보딩 + 7일 말씀 여정 — 기획 제안(`docs/features/onboarding-7day-journey.md`).
      헌법 충돌 3건(교단 해석 범위/데이터 저장/프롬프트·결제 승인) 해소 전 착수 불가, PROGRESS "사람 확인 필요" 참조
- [ ] [코드] 로그인 플로우 + 대화 이력 동기화
- [ ] [검증] 🚩DoD 로그인→대화→구절 렌더링 E2E 성공 (Android/iOS 양쪽)

## Phase 4 — 안전·품질 검증 (출시 게이트)
> CLAUDE.md Definition of Done의 하드 요구. 여기 통과 없이는 출시 없음.

- [ ] [코드] QA 러너: T1~T8 자동 실행 → theology-checker 판정 → 리포트 저장
- [ ] [검증] 🚩DoD 위기 시나리오 3종(직접/간접/혼합) 프로토콜 발동 확인
- [ ] [검증] 🚩DoD 환각 테스트: 존재하지 않는 구절 10건 전부 거부
- [ ] [검증] 🚩DoD 범위 밖 요청(코딩·숙제 등) 5건 정중 안내
- [ ] [검증] 🚩DoD T1~T8 QA 체크리스트 전부 통과 (theology-checker 기준)

## Phase 5 — 푸시 & 스토어 제출
> 출시 마무리. DoD의 스토어 항목.

- [ ] [민규] Apple Developer($99/년) / Google Play($25) 계정 등록
- [x] [민규] 서비스명 최종 확정 — 2026-07-16 앱=엠마오, 챗봇=바나바 (D-021). 내부 패키지는 malssumbeot 유지
- [ ] [코드] FCM/APNs 푸시 연동 (오늘의 말씀, 수신 동의 기반)
- [ ] [코드] 앱 내 신고 기능 (신고 → theology-checker 1차 분류 큐)
- [ ] [민규] 개인정보처리방침 + AI 생성 콘텐츠 고지 문안
- [ ] [민규] 성명표시권 표기 "성경전서 개역한글판, 대한성서공회" (앱 설정/정보 화면, D-016)
- [ ] [검증] 🚩DoD 스토어 제출물 준비 완료 (개인정보처리방침·AI 고지·신고 기능·스크린샷)
