# 말씀벗 스터디 노트

> 이 폴더는 프로젝트를 만들어가는 전 과정을 학습용으로 정리한 자료다.
> 단순히 "무엇을 했는지"가 아니라 **"왜 그렇게 했는지"** 를 중심으로 쓴다.
> 매 세션 작업이 끝날 때마다 갱신된다 (CLAUDE.md 세션 루틴 5번).

## 읽는 순서

| # | 문서 | 다루는 것 | 취업 직군 연관 |
|---|------|-----------|---------------|
| 01 | [프로젝트 셋업과 에이전트 운영 구조](01-project-setup.md) | 헌법/진행/결정 문서 체계, git·GitHub 셋업 | 공통 |
| 02 | [Spring Boot 스캐폴딩](02-spring-boot-scaffolding.md) | Initializr, Maven vs Gradle, 버전 선택, 설정 관리 | 서버개발 |
| 03 | [성경 DB와 Flyway 마이그레이션](03-bible-db-flyway.md) | 스키마 설계, 마이그레이션 버전 관리, 테스트 DB 전략 | 서버개발 |
| 04 | [BibleVerseService — 환각 방지 설계](04-bible-verse-service.md) | LLM 환각을 DB로 차단하는 구조, 파서, 단위 테스트 | 서버개발 + AI개발 |
| 05 | [Claude API 연동과 의도 분류기](05-claude-api-intent-classifier.md) | Messages API, 모델 라우팅/비용, LLM 출력 파싱의 안전 설계 | AI개발 |
| 06 | [CrisisFilter — 위기 감지 파이프라인](06-crisis-filter.md) | 결정론적 1차 방어선, sticky 상태, 안전 트레이드오프 | 서버개발 + AI개발 |
| 07 | [오케스트레이터 — 파이프라인 조립과 환각 차단](07-orchestrator-pipeline.md) | 프롬프트 분기, 모델 라우팅, 구절 검증·재생성·sanitize, 독립 검사 루프 | AI개발 |
| 08 | [개역한글 본문 확보 — 저작권 조사와 스크래핑](08-bible-text-sourcing.md) | 저작권 실사, 프로토타입 후 이식, DOM 기반 파싱, 데이터 검증 | 서버개발 + 법무 리스크 검토 |
| 09 | [파이프라인 메서드 호출 트리](09-pipeline-call-tree.md) | `handle()`부터 구절 검증까지 호출 흐름 추적 | AI개발 |
| 10 | [채팅 HTTP 계층 — `POST /api/chat`](10-http-api-layer.md) | DTO 분리, 단일 진입점으로 위기 우회불가, 슬라이스 vs 통합 테스트 | 서버개발 |
| 11 | [소셜 로그인 — OAuth 방식 A와 JWT](11-social-login-auth.md) | 제공자 토큰 검증, 전략 패턴, upsert, JWT 발급, 호출 트리 | 서버개발 + AI개발 |
| 12 | [성경 근거 파이프라인 재설계 — 검증된 원문을 "보고" 쓰게 만들기](12-grounding-pipeline-redesign.md) | grounding 미배선·미검증 우회·전체삭제 부작용 3종 세트, 2단계 생성 재설계 | AI개발 |
| 13 | [위기 sticky 재설계와 신학 검사 4연속 반려](13-crisis-sticky-redesign-and-review-loop.md) | 시간 기반→1회성 재설계, 독립 검사가 같은 프롬프트를 4번 잡아낸 과정 | 서버개발 + AI개발 |
| 14 | [Expo 스캐폴딩 — 배포 형태부터 이해하고 시작하기](14-expo-scaffolding.md) | Expo/Expo Go/빌드 구분, 실행 방식 3단계, SDK-폰 버전 결합, expo-router, 맥 없이 iOS | 모바일 |
| 15 | [채팅 UI 직접 만들기 — 라이브러리를 걷어낸 이유와 키보드에서 세 번 틀린 기록](15-chat-ui-and-keyboard.md) | 라이브러리 판단 기준, 동명 컴포넌트 함정, 추측을 없애는 설계, 리렌더 vs 애니메이션 경로, 시안의 시각 언어 | 모바일 |
| 16 | [모바일 로그인 플로우 — "저장은 됐는데 왜 다시 로그인 화면이 뜨지?"](16-mobile-login-flow.md) | Context vs SecureStore 역할 구분, 라우팅 타이밍 버그와 Stack.Protected, 기술 제약으로 범위를 미루는 판단 | 모바일 |
| 17 | [탭과 옵트인 저장 — 화면 구조와 데이터 책임을 함께 바꾸기](17-tabs-and-opt-in-storage.md) | Stack과 Tabs의 역할 분리, 자동 저장과 명시적 저장의 차이, JWT 소유권 검사, 구절 스냅샷 | 모바일 + 서버개발 |

## 이 프로젝트에서 배울 수 있는 큰 그림

말씀벗은 "LLM 위에 가드레일을 쌓는" 전형적인 **AI 응용 서비스**다. 핵심 기술 스토리는 세 가지:

1. **신뢰할 수 없는 출력 다루기** — LLM은 성경 구절을 지어낼 수 있다(환각).
   해법: 모델에게는 구절 *주소*만 제안하게 하고, 본문은 서버가 DB에서 조회한다 (04장).
2. **안전이 기능보다 우선** — 위기 신호(자살·자해)는 어떤 로직보다 먼저 처리한다.
   해법: 결정론적 필터를 LLM 호출 *앞에* 두고, LLM 분류는 2차 방어선으로만 쓴다 (05~06장).
3. **만든 자와 검사하는 자 분리** — 자기가 만든 프롬프트를 자기가 검증하면 못 본다.
   해법: 독립 검사 에이전트(theology-checker)가 비판적으로 평가하고, 사람이 최종 승인한다 (01·05장).
