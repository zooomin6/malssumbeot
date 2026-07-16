# 10. 채팅 HTTP 계층 — `POST /api/chat` (Phase 1)

지금까지 `ChatOrchestrator.handle()`은 "잘 도는 두뇌"였지만 **밖에서 부를 문**이 없었다.
Phase 1은 그 문을 다는 작업이다. 새 패키지 `com.malssumbeot.api`에 DTO 2개와 컨트롤러 1개.

## 1. 왜 DTO를 도메인 객체와 따로 두나

컨트롤러가 `ChatReply`(도메인 레코드)를 그대로 반환할 수도 있었다. 하지만 나눴다:

- **API 계약과 내부 모델을 분리**한다. `Intent`는 내부 enum인데, API에는 문자열(`"DAILY_CHAT"`)로
  노출한다. 내부 enum 이름을 리팩토링해도 API 계약이 깨지지 않게 하는 얇은 완충재다.
- `ChatResponse.from(ChatReply)`라는 **한 곳**에서만 변환한다. 매핑 로직이 흩어지지 않는다.
- `passages`는 `VersePassage`를 그대로 노출하지 않고 API용 `Passage`(reference/bookName/장·절/
  구조화된 verse 리스트)로 다시 담는다. RN 앱이 **인용 블록을 프로즈와 구분해 렌더링**하려면
  절 단위 구조가 필요하기 때문(DoD의 "구절 인용 블록 구분 렌더링").

> 핵심 원칙은 그대로다: 성경 본문은 DB 검증을 거친 `passages`에만 담긴다(D-003). 모델이 만든
> 구절 본문이 API로 새어나가는 경로는 없다.

## 2. 위기 우회 불가(D-004)를 "인터셉터" 없이 지키는 법

ROADMAP엔 "`CrisisFilter`를 Spring 인터셉터로 배선"이라 적혀 있었다. 그런데 막상 설계하니
인터셉터가 **어색한 도구**였다:

- 위기는 단순히 "막는" 게 아니라 **응답(카테고리·레벨별 문구)을 만들어 돌려줘야** 한다.
  인터셉터가 그걸 하려면 오케스트레이터의 위기 로직을 복제해야 한다.
- 인터셉터(`preHandle`)에서 요청 바디를 읽으면 스트림이 소비돼 컨트롤러가 다시 못 읽는다.
  `ContentCachingRequestWrapper` + 별도 필터가 필요 — MVP에 과한 배관.
- 오케스트레이터가 이미 **첫 줄에서** `crisisFilter.check()`를 부른다. 이중 감지가 된다.

그래서 택한 것: **단일 진입점(single choke point)**. 채팅 트래픽이 `ChatController` 하나만
거치고, 그게 `handle()`(위기 우선)만 부른다면, 위기 분기는 구조적으로 우회 불가다.
"우회 불가"의 목표는 인터셉터라는 *수단*이 아니라 **위기 감지를 건너뛰는 경로가 없다**는
*상태*다. 지금은 단일 진입점이 그 상태를 더 단순하게 달성한다.

트레이드오프: 엔드포인트가 여러 개(`/api/prayer` 등)로 늘면, 새 경로가 위기 감지를 안 거칠
위험이 생긴다. 그때 인터셉터로 방어선을 한 겹 더 올린다. 지금 만들지 않는 이유는 "투기적
코드 금지" — 아직 없는 두 번째 엔드포인트를 위한 배관은 짓지 않는다.

이 결정이 말로만 끝나지 않게, **통합테스트**로 못 박았다:

```java
@SpringBootTest @AutoConfigureMockMvc
class ChatApiIntegrationTest {
    @MockitoBean ClaudeChat claudeChat;               // 외부 모델만 모킹

    @Test void 위기_메시지는_HTTP_경로에서도_모델없이_위기_프로토콜로() {
        // POST /api/chat {"message":"죽고 싶어"} → crisis=true, text에 109 포함
        verifyNoInteractions(claudeChat);             // 모델을 절대 안 부른다
    }
}
```

`verifyNoInteractions(claudeChat)`가 핵심이다: 위기 요청이 의도 분류·응답 생성(둘 다 모델 호출)을
거치지 않고 결정론적 위기 프로토콜로 갔음을, HTTP 경로 전체를 태워 증명한다.

## 3. 테스트 두 겹: 슬라이스 vs 통합

- **`@WebMvcTest(ChatController.class)`** — 웹 계층만 띄우고 오케스트레이터는 `@MockitoBean`.
  빠르다(DB·모델 컨텍스트 없음). HTTP 매핑과 입력 검증(빈 메시지·sessionId 누락 → 400)만 본다.
- **`@SpringBootTest`** — 전체 컨텍스트를 실제 빈으로. 위기 우회 불가라는 *횡단 관심사*는
  실제 배선으로만 증명되므로 여기서 검증한다. 외부 모델(`ClaudeChat`)만 잘라낸다.

슬라이스로 될 일을 통합테스트로 하면 느려지고, 통합으로 증명할 일을 슬라이스로 하면 거짓
안심이 된다. "무엇을 증명하려는가"로 테스트 종류를 고른다.

> `@MockBean`이 아니라 `@MockitoBean`을 쓴다 — Spring Boot 3.4+에서 `@MockBean`은 deprecated,
> `org.springframework.test.context.bean.override.mockito.MockitoBean`이 후속이다.

## 4. sessionId를 왜 바디로 받나

위기 sticky(D-012/D-020)가 **세션 단위**라 요청마다 세션 식별자가 필요하다. 헤더(`X-Session-Id`)
vs 바디 필드 중 **바디 필드**를 택했다: 인증(Phase 2) 도입 전 MVP에선 가장 단순하고 명시적이며,
curl·RN 양쪽에서 다루기 쉽다. 인증이 들어오면 세션/사용자 식별은 토큰 기반으로 옮겨간다.

## 5. 남은 일

- 인증(Phase 2): 소셜 로그인 → 사용자·세션 식별을 토큰으로. `CrisisSessionStore`도 인메모리 →
  Redis/DB(다중 서버·영속화).
- 전수 E2E(위기 3종·구절 인용·환각 거부 각 다수)는 Phase 4 QA 러너가 **같은 `/api/chat`**으로 수행.
- 엔드포인트가 늘면 위기 인터셉터 재검토.
