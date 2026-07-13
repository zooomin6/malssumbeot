# 09. 파이프라인 메서드 단위 호출 트리

> 예시 메시지 "개같은 상사 때문에 너무 힘들어 어떻게 해야할지 모르겠어"가
> 파이프라인을 통과하는 과정을 메서드 호출 순서 그대로 따라간 기록.
> (2026-07-13 민규 학습 세션 정리)

## 전체 호출 트리

```
ChatOrchestrator.handle(sessionId, "개같은 상사 때문에...")
│
├─ ① CrisisFilter.check(sessionId, message)
│   ├─ CrisisDetector.detect(message)
│   │   └─ 공백 제거("죽 고 싶 어" 변형 대응) 후 패턴 목록을 순회하며 정규식 매칭
│   │       → 매칭 없음 → Optional.empty()
│   ├─ (매칭됐다면) CrisisSessionStore.mark(sessionId) → CrisisCheck.newSignal(...)
│   ├─ CrisisSessionStore.isActive(sessionId)
│   │   └─ 이 세션이 최근에 위기로 표시됐는지 + 유지시간이 안 지났는지 확인 → false
│   └─ 반환: CrisisCheck.none()  →  crisis()==false이므로 계속 진행
│
├─ ② IntentClassifier.classify(message)
│   ├─ ClaudeChat.complete(경량모델, maxTokens=16, 분류프롬프트, message)   ← LLM 호출 1회
│   │   └─ AnthropicClaudeChat.complete(): SDK로 API 호출, 텍스트 블록 이어붙여 반환
│   │       → "상담"
│   ├─ label.contains("위기") 체크 → 아님 (2차 방어선, 형식 어겨도 위기는 잡음)
│   └─ Intent.fromLabel("상담") → Intent.COUNSELING
│       (해석 실패 시 폴백도 COUNSELING — 가장 보수적인 모드)
│
├─ ③ ChatOrchestrator.generate(COUNSELING, message)
│   │
│   ├─ ModelRouter.route(COUNSELING)
│   │   └─ switch문: 상담·기도문·QA·위기 → faith-model / 일상·범위밖 → casual-model
│   │       → faith-model 반환
│   │
│   ├─ PromptAssembler.assemble(COUNSELING, List.of())
│   │   ├─ PromptRepository.master()            → master.txt (부팅 때 로드해둔 것)
│   │   ├─ PromptRepository.forIntent(COUNSELING) → counseling.txt
│   │   └─ 둘을 이어붙여 시스템 프롬프트 문자열 반환 (구절 목록이 비어서 <bible_verses>는 생략)
│   │
│   ├─ ClaudeChat.complete(faith-model, 1024, 시스템프롬프트, message)      ← LLM 호출 2회
│   │   → "많이 지치셨겠어요... 빌립보서 4:6-7 말씀이 위로가 되기를..."
│   │
│   ├─ ④ ChatOrchestrator.verify(text)          ← 구절 검증
│   │   ├─ VerseReferenceScanner.scan(text)
│   │   │   └─ 정규식으로 주소 후보 탐색, 후보마다:
│   │   │       ├─ "~장/~편"으로 끝나면 → VerseReferenceParser.parseChapter(후보)
│   │   │       ├─ 아니면              → VerseReferenceParser.parse(후보)
│   │   │       │   ├─ 정규식으로 book/chapter/start/end 분해
│   │   │       │   └─ BibleBookCatalog.resolve("빌립보서")
│   │   │       │       └─ 이름·약어 맵 조회 → BibleBook 반환 (모르는 이름이면 예외)
│   │   │       └─ 해석 성공한 후보만 결과에 추가 (중복 제거, 등장 순서 유지)
│   │   │   → ["빌립보서 4:6-7"]
│   │   │
│   │   └─ 주소마다: BibleVerseService.findPassage("빌립보서 4:6-7")
│   │       └─ getPassage() 를 try로 감싼 것 (예외 → Optional.empty)
│   │           ├─ VerseReferenceParser.parse(...)   ← Scanner 때와 별개로 다시 파싱
│   │           ├─ 장 번호가 그 책의 장 수를 넘는지 검사 (넘으면 VerseNotFoundException)
│   │           ├─ BibleVerseRepository.findByBookIdAndChapterAndVerseBetween...(...)
│   │           │   └─ SQL: 해당 책 id + 4장 + 6~7절 행만 SELECT
│   │           ├─ 행 수 < 요청한 절 수면 VerseNotFoundException (환각 의심)
│   │           └─ VersePassage(주소, 책이름, 장, 절범위, 원문 줄들) 생성
│   │       → passages=[빌립보서 4:6-7 원문], unverified=[]
│   │       (DB에 없었다면: findPassage가 empty → hasChapter()로 장 인용인지 재확인
│   │        → 그것도 아니면 unverified에 추가)
│   │
│   ├─ ⑤ unverified가 비어있지 않으면 (환각 재시도 — 이번엔 해당 없음)
│   │   ├─ 원래 메시지 + <system_note>없는 구절 목록</system_note> 를 붙여서
│   │   ├─ ClaudeChat.complete(...) 1회 재생성                              ← LLM 호출 (조건부)
│   │   └─ verify(text) 다시 실행 (재검증)
│   │
│   ├─ ⑥ references가 하나라도 있으면 → 모델 텍스트를 버림 (D-017)
│   │   ├─ passages 있음 → text = ""       (DB 원문만 별도 필드로 전달)
│   │   └─ passages 없음 → text = 환각 안내 문구
│   │
│   └─ 반환: new ChatReply(text, COUNSELING, false, passages, unverified)
```

## 트리만 봐서는 안 보이는 포인트

### 부팅 시점 vs 요청 시점

트리의 호출은 메시지마다 실행되지만, 재료 준비는 앱 시작 때 한 번만 된다.
`PromptRepository` 생성자가 `prompts/*.txt`를 전부 읽어 메모리에 올려두고,
`CrisisDetector` 생성자가 위기 패턴을 컴파일해두고, `BibleBookCatalog` 생성자가
66권 이름·약어 맵을 만들어 둔다. **왜**: 요청 처리 중 파일 I/O를 없애서
매 메시지 처리 비용을 맵 조회·정규식 매칭 수준으로 낮추기 위해.

### Parser가 두 번 불리는 이유

Scanner 안에서 한 번(주소 형태인지 거르는 용도, 결과는 버림),
BibleVerseService 안에서 또 한 번(DB 조회 조건을 만드는 용도).
Scanner가 Service에 문자열만 넘기는 구조라서 생기는 중복이다.

### LLM 호출 횟수

- 정상 경로: 메시지 1건당 2회 (분류 1회 + 생성 1회)
- 환각 감지 시: +1회 재생성 → 최대 3회
- 위기 경로: 0~1회 (1차 필터에 잡히면 0회, 분류기가 잡으면 분류 1회만).
  **왜**: 위기 응답은 `CRISIS_FALLBACK_TEXT` 고정 문구라 모델을 부르지 않는다 —
  위기 상황에서 모델 오작동 가능성까지 배제하는 결정론적 안전망.

### ClaudeChat은 인터페이스

분류기·오케스트레이터가 쓰는 `claudeChat.complete(...)`는 인터페이스 호출이고
구현은 `AnthropicClaudeChat` 하나. **왜**: 테스트에서 가짜(fake) 구현을 꽂아
API 호출 없이 파이프라인을 검증하기 위해.

## 학습 Q&A (세션에서 나온 질문들)

### sessionId는 뭔가?

"어느 대화방에서 온 메시지인가"를 구분하는 식별자. sticky 위기 상태가
"**이 대화**가 위기 상태다"라는 기록이므로, 세션 구분 없이 전역으로 기록하면
사용자 A의 위기가 무관한 B의 대화까지 위기 모드로 만들어 버린다.
지금은 호출자(테스트)가 넘겨주는 문자열이고, 채팅 REST API가 생기면
로그인 세션/대화방 ID가 들어온다.

### mark() vs isActive() — 쓰기와 읽기

- `mark(sessionId)` = **쓰기**. 위기 신호 감지 순간 "이 세션, 방금 위기였음"을
  현재 시각과 함께 기록.
- `isActive(sessionId)` = **읽기**. "기록이 있고 아직 유지시간(30m) 안인가?"를
  확인만 한다. 활성화시키는 메서드가 아니다.
- 두 메서드는 같은 턴에 함께 불리지 않는다: 신호가 잡히면 `mark()` 후 즉시 반환,
  `isActive()`는 이번 메시지에 신호가 **없을 때만** 확인용으로 불린다.

| 턴 | 메시지 | detect | 실행되는 것 | 결과 |
|---|---|---|---|---|
| 1 | "다 끝내고 싶어" | 신호 있음 | `mark()` 기록 | NEW_SIGNAL — 위기 응답 |
| 2 | "면접 기도문 써줘" | 신호 없음 | `isActive()` → true | STICKY — 여전히 위기 응답 |
| 3 | (유지시간 지난 뒤) "안녕" | 신호 없음 | `isActive()` → false | NONE — 일반 파이프라인 |

### complete()의 인자 4개

`complete(model, maxTokens, systemPrompt, userMessage)`

- **model**: 모델명 문자열이 들어가는 자리. `application.yml`의
  `classifier-model: claude-haiku-4-5` 값을 Spring이 `@Value`로 주입.
  **왜 설정으로 뺐나**: 모델 교체 시 코드가 아닌 yml 한 줄만 고치면 되도록.
- **maxTokens**: 모델 출력의 최대 길이(토큰) 상한. 분류 호출은 라벨 단어 하나만
  필요해서 16 — 모델이 장황하게 설명을 붙여도 강제로 잘리고, 비용 안전장치도 겸함.
  답변 생성 호출은 1024.
- **systemPrompt**: 우리가 작성한 지시문 (`prompts/intent-classifier.txt` 등).
- **userMessage**: 사용자 메시지 원본 그대로.

### complete라는 이름의 뜻

"완성됐다"가 아니라 "(텍스트를) 완성해 달라"는 동사. LLM 용어 completion에서 온
이름으로, "프롬프트를 보내고 모델의 응답 텍스트를 문자열로 받아오는 함수"다.
`AnthropicClaudeChat.complete()` 내부:

1. `MessageCreateParams.builder()...build()` — API 요청서 조립 (아직 통신 없음)
2. `client.messages().create(params)` — 실제 API 호출 (유일하게 네트워크 타는 줄)
3. `response.content().stream()...` — 응답은 블록 리스트(텍스트 블록 외 다른 종류도
   가능)라서, 텍스트 블록만 골라 이어붙여 순수 문자열로 반환

### label과 Intent

- `label` = `IntentClassifier.classify()` 안의 지역 변수 (IntentClassifier.java:41).
  `complete()`가 반환한 모델 출력 문자열("상담")을 담는다.
- `Intent` = 사용자 의도 6분류를 나타내는 enum. **왜 문자열 대신 enum인가**:
  오타·변형이 컴파일 단계에서 차단되고, switch에서 분기 누락 시 컴파일러가 잡아준다.
- 방향: 라벨을 달아 나가는 게 아니라, 모델이 출력한 **불안정한 문자열을**
  코드 내부의 **안전한 타입으로 들여오는** 변환이다.
  `"상담"` ─`Intent.fromLabel()`─▶ `Intent.COUNSELING`
- 변환된 Intent 하나가 이후 분기를 전부 결정: 위기 여부 확인, 모델 라우팅,
  모드 프롬프트 선택.

### handle → generate 역할 분담

`handle()`은 문지기(위기 검사 + 의도 딱지 붙이기)까지만 하고,
`generate(intent, userMessage)`에 딱지와 원본 메시지를 함께 넘긴다.
intent는 모델·프롬프트 선택에, 원본 메시지는 실제 생성 요청에 쓰인다.
`sessionId`는 위기 기록에만 필요해서 generate까지 따라가지 않는다.

### 매 채팅마다 전부 도는가?

그렇다. `handle()`은 메시지 1건마다 호출되고 위기 감지→분류→조립→생성→검증이
매번 다시 돈다. **왜**: 3번째 메시지에서 갑자기 위기 신호가 올 수도, 일상 대화가
기도문 요청으로 바뀔 수도 있어서. 대화를 넘어 유지되는 유일한 상태는
세션별 위기 sticky 기록뿐이다.
