# 07. 오케스트레이터 — 파이프라인 조립과 환각 차단

앞의 부품들(위기 필터, 의도 분류기, 성경 검증, 프롬프트)을 하나로 잇는 곳이다.
이 서비스의 "심장"이며, 헌법이 정한 순서를 그대로 코드로 옮긴다.

## 1. 파이프라인 순서는 헌법이다

```
사용자 메시지
   │
   ▼
① CrisisFilter      위기면 즉시 위기 프로토콜 (분류 건너뜀)
   │ (통과)
   ▼
② IntentClassifier  6분류. 위기로 나오면 sticky 기록 후 위기 프로토콜
   │
   ▼
③ PromptAssembler   마스터 + 모드 분기 프롬프트 조립
   │
   ▼
④ ModelRouter       신앙=Sonnet / 일상=Haiku
   │
   ▼
⑤ ClaudeChat        응답 생성
   │
   ▼
⑥ 구절 검증         스캔 → DB 검증 → (환각 시) 재생성 → sanitize
```

이 순서(`위기 → 분류 → 분기 → 생성 → 검증`)는 CLAUDE.md에 박힌 컨벤션이라
바꾸거나 우회할 수 없다. [ChatOrchestrator.handle](../../backend/src/main/java/com/malssumbeot/orchestrator/ChatOrchestrator.java)이
이 흐름을 그대로 따른다.

## 2. 위기가 "모든 것에 우선"한다는 걸 코드로 표현하기

```java
public ChatReply handle(String sessionId, String userMessage) {
    CrisisCheck check = crisisFilter.check(sessionId, userMessage);
    if (check.crisis()) {
        return crisisReply(userMessage);   // 분류기를 아예 호출하지 않음
    }
    Intent intent = classifier.classify(userMessage);
    if (intent == Intent.CRISIS) {
        crisisFilter.recordCrisis(sessionId);  // 2차 방어선 판정도 sticky에 반영
        return crisisReply(userMessage);
    }
    return generate(intent, userMessage);
}
```

두 가지 안전 설계가 들어있다:
- **1차(필터)가 잡으면 분류기를 호출조차 안 한다.** 위기 신호에 LLM 호출을 한 번이라도
  끼우면, 그 호출이 실패할 때 위기 처리가 막힌다. 결정론적 경로를 먼저 둔다.
- **2차(분류기)가 위기로 판정하면 `recordCrisis`로 세션을 sticky 상태로 만든다.**
  06장의 멀티턴 문제를, 필터가 직접 못 잡고 LLM이 잡은 경우까지 확장한 것.

## 3. 위기 응답마저 실패하면? — 결정론적 폴백

위기일 때도 응답은 LLM이 생성한다(따뜻한 문장 + 109 안내). 그런데 그 API 호출이
실패하면? 위기 사용자에게 에러를 보여줄 수는 없다.

```java
private ChatReply crisisReply(String userMessage) {
    try {
        return generate(Intent.CRISIS, userMessage);
    } catch (RuntimeException e) {
        log.error("위기 응답 생성 실패 — 결정론적 폴백으로 응답", e);
        return new ChatReply(CRISIS_FALLBACK_TEXT, Intent.CRISIS, true, List.of(), List.of());
    }
}
```

`CRISIS_FALLBACK_TEXT`는 109·1577-0199 연락처가 박힌 고정 문자열이다. 외부 의존성이
0이라 무슨 일이 있어도 사용자에게 전화번호는 전달된다. **가장 중요한 경로일수록
가장 단순한 폴백을 둔다** — 안전 시스템 설계의 핵심.

## 4. 환각 차단 3단계 (이 장의 하이라이트)

04장에서 "모델은 구절 주소만, 본문은 DB가"라는 원칙을 세웠다. 오케스트레이터가 이걸
실제 응답 흐름에서 강제한다.

```
1. 생성        모델이 "빌립보서 4:6-7" 같은 주소를 텍스트에 포함
2. 스캔·검증   VerseReferenceScanner가 주소를 뽑고 BibleVerseService가 DB 대조
3. 재생성      존재하지 않는 주소가 있으면(환각) → "그 구절은 없으니 빼라"는
               system_note를 붙여 1회 다시 생성
4. sanitize    재생성에도 환각이 남으면 → 그 주소가 든 문장을 본문에서 제거
```

### 왜 sanitize(4단계)가 추가됐나 — 독립 검사가 잡아낸 결함

처음엔 3단계까지만 만들고 "재생성 후에도 환각이 남으면 그대로 보내되 미검증 목록에
표시"했다. 신학 검사 에이전트가 이걸 **C1 FAIL**로 판정했다:

> 인용 블록을 안 붙이는 것만으로는, 응답 *본문 문장*에 박힌 "빌립보서 9:9"(존재하지
> 않는 구절)가 사용자에게 그대로 노출되는 걸 못 막는다. T8 기준은 '거부'이지
> '표시 후 전송'이 아니다.

맞는 지적이라 코드를 고쳤다:

```java
if (!verification.unverified().isEmpty()) {
    String sanitized = stripSentencesContaining(text, verification.unverified());
    text = sanitized.isBlank() ? HALLUCINATION_FALLBACK_TEXT : sanitized;
}
```

`stripSentencesContaining`은 환각 주소가 든 **문장을 통째로** 제거한다. 주소만 지우면
"  말씀처럼 평안을"같은 어색한 잔해가 남고, 더 중요하게는 그 문장에 모델이 풀어쓴
가짜 본문도 같이 있을 수 있어서다. 문장 전체를 들어내면 둘 다 사라진다. 다 들어내서
본문이 비면 안전 문구로 대체한다.

이때 안전 vs 정확의 트레이드오프: 검증된 주소와 환각 주소가 *한 문장*에 섞이면 멀쩡한
주소까지 지워진다. 그래도 **과하게 지우는 쪽**을 택했다 — 환각이 새는 것(미탐)이
멀쩡한 구절 하나 빠지는 것보다 훨씬 나쁘기 때문. 06장의 오탐/미탐 비대칭과 같은 논리.

### 남은 한계 (정직하게 기록)

`VerseReferenceScanner`는 처음에는 "책 장:절" 패턴만 잡았다. 그래서 "시편 23편"처럼
장만 언급한 인용이 스캔 대상에서 빠졌고, "요한복음 99장" 같은 환각도 검증할 수 없었다.
이후 장 단위 패턴을 추가하고, `BibleVerseService`가 `bible_book.chapter_count`로 해당 장의
존재를 확인하게 했다.

그런데 주소가 존재한다고 해서 주소 주변의 모델 문장까지 신뢰할 수 있는 것은 아니다. 예를 들어
모델이 `시편 23편은 "..."라고 말합니다`라고 답하면, 장 자체는 실제로 존재해도 따옴표 안의
본문은 모델 기억일 수 있다. 처음에는 성경 주소가 포함된 문장만 제거하려 했지만, 주소가 첫 문장에
있고 모델 본문이 다음 문장에 이어지면 본문이 남는 문제가 있었다. 그래서 D-017에서는 **성경 주소를
하나라도 발견하면 모델 텍스트 전체를 전송하지 않기**로 했다. 장·절 주소의 본문은
`BibleVerseService`가 조회한 `VersePassage`만 UI 인용 블록으로 전달한다. 장 단위 주소는 특정
절 원문을 정할 수 없으므로, DB 기반 장 인용 UI를 설계하기 전까지 안전 폴백으로 처리한다.

위기 응답은 더 강하게 다룬다. 위기 모델 응답에 `시편 34:18` 같은 주소가 포함되면 일반 응답과
같은 차단 규칙이 109·1577-0199 연락처까지 지울 수 있다. 그래서 위기 분기는 모델 호출을 하지
않고, 연락처가 포함된 결정론적 안내문을 항상 반환한다. 또한 모델이 `John 3:16`처럼 영어 주소를
쓸 가능성도 있으므로, DB에서 해석하지 못하는 영어 장절 표기 역시 환각 후보로 감지해 제거한다.

주소 없이 본문만 풀어쓴 경우는 여전히 잡지 못한다. RAG(임베딩 검색) 도입 전이라
`<bible_verses>` 블록이 비어 있어 프롬프트 지시에 의존하는 부분도 남아 있다. **완벽하지 않은
방어선도, 한계를 아는 채로 쓰면 안전하다. 모르는 채로 쓰는 게 위험하다.**

## 5. 만든 자와 검사하는 자의 분리가 실제로 작동한 사례

이 세션에서 신학 검사 에이전트는 두 번 FAIL을 냈고(분류 프롬프트의 위기 강등, 그리고
이 장의 환각 본문 노출), 둘 다 내가 처음 만들 때 못 본 결함이었다. 자기 코드를 자기가
검증하면 자기 가정에 갇힌다(D-005). 비판만 하는 독립 검사자를 따로 두는 구조가
값을 한다는 증거다. 단, 검사자가 제안한 **프롬프트 수정**은 사람 승인 항목이라 바로
반영하지 않고 PROGRESS.md에 올렸다 — 코드는 내가, 신학 판단은 사람이.
