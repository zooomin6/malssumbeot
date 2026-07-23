# 12. 성경 근거 파이프라인 재설계 — 검증된 원문을 "보고" 쓰게 만들기 (D-025)

> 외부 코드 리뷰(다른 AI 세션이 진행)가 발견한 문제를 이 세션에서 직접 소스 코드와
> 대조해 재검증하고, 승인받은 뒤 고쳤다. "만든 자와 검사하는 자를 분리한다"는 원칙(D-005)이
> 리뷰 자체에도 한 번, 이 문서 작성에도 한 번 적용된 사례다.

## 배경 — 왜 이 문제를 다시 보게 됐나

07장에서 "환각 차단 3~4단계"를 만들었다고 정리했었다. 정규식으로 구절 주소를 찾고, DB로
검증하고, 검증 실패하면 재생성하거나 문장을 지운다는 흐름이었다. 이 자체는 잘 작동했지만,
독립 리뷰에서 **그보다 앞단**에 세 가지 구조적 문제가 있다는 게 드러났다.

---

## 문제 상황 1 — 모델이 성경 원문을 한 번도 "보지" 못하고 생성한다 (P0-1)

### 당시 코드

```java
// PromptAssembler.java — <bible_verses> 블록을 만드는 기능은 이미 있었다
public String assemble(Intent intent, List<VersePassage> verifiedPassages) {
    StringBuilder sb = new StringBuilder(prompts.master());
    sb.append("\n\n").append(prompts.forIntent(intent));
    if (verifiedPassages != null && !verifiedPassages.isEmpty()) {
        sb.append("\n\n<bible_verses>\n");
        // ... 검증된 원문을 여기 채운다
    }
    return sb.toString();
}
```

```java
// ChatOrchestrator.java — 그런데 실제 호출부가 이랬다
String system = promptAssembler.assemble(intent, List.of());  // 항상 빈 리스트!
```

`assemble()`은 완벽하게 잘 만들어져 있었고 단위 테스트(`PromptAssemblerTest`)까지 있었다.
그런데 실제로 이 메서드를 호출하는 곳이 **딱 한 곳**뿐이었고, 그 한 곳이 항상 빈 리스트를
넘기고 있었다. 기능은 있는데 배선이 안 된, "죽은 코드"였다.

master.txt는 모델에게 "본문 내용은 `<bible_verses>` 안의 원문만 사용하라"고 지시하는데,
정작 그 블록이 실제로는 절대 채워지지 않았던 것이다. 모델은 어쩔 수 없이 자기 기억으로
성경 내용을 지어내 쓸 수밖에 없었다 — CLAUDE.md 절대원칙 2번("모든 신앙적 답변은 성경
DB 원문을 근거로 한다")을 코드 차원에서 어기고 있었던 셈이다.

### 왜 아무도 못 알아챘나

기존 검증(07장의 3~4단계)은 **생성 이후**에 주소를 스캔해서 검증하는 방식이라, "모델이
원문을 보고 썼는지"는 애초에 검사 대상이 아니었다. 사후 검증과 사전 grounding은 완전히
다른 문제인데, 사후 검증이 있으니 괜찮다고 착각하기 쉬웠다.

---

## 문제 상황 2 — 구절 주소가 없는 신앙적 주장은 검증을 완전히 우회한다 (P0-2)

### 당시 코드

```java
private Verification verify(String text) {
    List<String> references = scanner.scan(text);  // 정규식으로 "책이름 장:절" 패턴만 찾음
    // ... references가 비어있으면 아래 로직 전체가 안 돈다
}
```

모델이 "빌립보서 4:6-7 말씀처럼..."이라고 쓰면 검증 대상이 된다. 그런데 모델이 그냥
"성경은 우리가 사랑받는 존재라고 말합니다"처럼 **주소 없이** 신앙적 주장만 하면? 정규식이
아예 매치할 게 없으니 `references`가 비어있고, 검증 로직 자체가 실행되지 않는다.
100% 무검증 통과.

### 왜 이게 진짜 문제인가

기존 D-017("주소 있으면 전체 차단")과 이번 문제는 겹치는 것 같지만 다르다. D-017은
"주소가 있는데 그 옆 본문을 못 믿을 때"를 막는 장치고, 이번 문제는 "애초에 주소 자체가
없는 경우"라 D-017의 방어선에 걸리지도 않는다. 두 문제를 합치면: 이 파이프라인은
"그럴듯한 위로 문장(검증 안 됨)" 또는 "코멘트 없는 날것의 성경 구절"만 만들 수 있고,
원래 프롬프트가 의도한 "공감 + 검증된 근거"라는 정상 케이스는 구조적으로 나올 수 없었다.

---

## 문제 상황 3 — 검증에 "성공"해도 모델의 공감 코멘트가 통째로 삭제된다 (P0-3)

### 당시 코드

```java
// D-017 당시 도입된 로직
if (!verification.references().isEmpty()) {
    text = verification.passages().isEmpty() ? HALLUCINATION_FALLBACK_TEXT : "";
    //                                                                        ^^ 성공해도 빈 문자열!
}
```

이 한 줄이 이번 세션 전체에서 가장 체감이 컸던 문제였다. "주소가 하나라도 스캔되면"
성공이든 실패든 **묻지도 따지지도 않고 텍스트를 지웠다.** 검증이 실패했을 때만 지우는 게
아니라, 검증에 **성공**했을 때도 지웠다. 그러니 사용자는 모델이 애써 쓴 공감 문장은 하나도
못 받고 DB에서 조회한 성경 구절 원문만 뚝 떨어진 응답을 받고 있었다.

### 어떻게 발견했나 — 실제 시뮬레이션

이 문제는 코드만 봐서는 "그런 게 있구나" 정도였는데, 실제 대화 예시로 시뮬레이션해보니
훨씬 뚜렷하게 보였다. 사용자가 "사고로 시력을 잃었는데 오늘 설교에서 요한복음 9장(날 때부터
맹인 된 사람) 이야기를 들었다. 정죄감이 안 없어진다"는 긴 고민을 보냈다고 하자.

모델이 실제로는 이렇게 잘 답했다고 치자:

```
많이 힘드셨겠어요. 사고로 시력을 잃으신 것에 대해 정죄감까지 느끼신다니...
오늘 설교 말씀처럼, 이건 누구의 잘못도 아니에요. [공감 + 적용 문장들]
[요한복음 9:1-3]
```

그런데 검증 로직이 "요한복음 9:1-3"이라는 실재하는 주소를 발견하는 순간, 저 위의 공감
문장을 전부 지우고 `[요한복음 9:1-3]` 원문 텍스트만 남겼다. **검증이 정확히 성공한
케이스에서도** 이 부작용이 터졌다 — 심지어 이걸 검증하는 테스트(`ChatOrchestratorTest`의
"구절을 DB로 검증해 원문을 첨부한다" 테스트)조차 `text`가 빈 문자열이 되는 걸 알면서도
그 값 자체를 assert하지 않고 넘어가고 있었다. 버그가 테스트 안에 숨어 있었던 셈이다.

---

## 현재 상황 — 2단계 grounded 생성으로 재설계 (D-025)

세 문제를 한 번에 푸는 대안 두 가지를 놓고 비교했다.

| | A안: 단일 호출 + 태그 분리 | B안: 2단계 재생성 (채택) |
|---|---|---|
| 호출 횟수 | 1회 | 2회 |
| 비용 증가(추정) | +7%대 | +20~25% |
| 지연 증가 | 거의 없음 | 기존에도 2왕복이 기본이었어서 재계산하니 +1~2초 |
| 모델이 원문을 실제로 "보는가" | 아니오(사후 조립) | **예** |

처음엔 A안(비용·지연 유리)이 나아 보였는데, 실제 지연 증가폭을 다시 계산해보니(이미
"인텐트분류→본생성"이 기본 2왕복 구조였음) B안의 단점이 생각보다 작았다. "모델이 실제
원문을 보고 쓴다"는 게 신앙 답변 정확성에서 훨씬 중요하다고 판단해 **B안**으로 확정.

### 개선 코드

**1단계 — 신규 프롬프트로 주소만 제안받기** (`verse-address-proposal.txt`, 경량 모델)

```
당신은 사용자 메시지와 관련된 성경 구절 주소를 제안하는 내부 보조 도구입니다.
...
- 확실히 존재한다고 알고 있는 구절 주소만 제안하세요. 확신이 없으면 아무것도 쓰지 마세요.
- 구절 본문, 설명, 인사말은 절대 쓰지 않습니다. 오직 주소만 줄바꿈으로 나열하세요.
```

**서버가 DB로 검증 → 검증된 것만 2단계로**

```java
private List<VersePassage> proposeVerifiedPassages(String userMessage) {
    String proposal = claudeChat.complete(
            proposalModel, PROPOSAL_MAX_TOKENS, prompts.verseAddressProposal(), userMessage);
    List<VersePassage> passages = new ArrayList<>();
    for (String line : proposal.lines().toList()) {
        String candidate = line.strip();
        if (!candidate.isBlank()) {
            verseService.findPassage(candidate).ifPresent(passages::add);  // DB에 없으면 조용히 버림
        }
    }
    return passages;
}
```

**2단계 — 검증된 원문을 실제로 프롬프트에 실어서 생성** (P0-1 해결)

```java
private ChatReply generate(Intent intent, String userMessage) {
    String model = modelRouter.route(intent);
    List<VersePassage> verifiedPassages = intent.requiresGrounding()
            ? proposeVerifiedPassages(userMessage)
            : List.of();  // 일상대화·범위밖은 애초에 1단계를 안 돈다 (비용 절약)

    String system = promptAssembler.assemble(intent, verifiedPassages);  // 드디어 진짜로 채워짐!
    String text = claudeChat.complete(model, MAX_TOKENS, system, userMessage);
    ...
}
```

**검증된 구절이 없으면 "인용 금지" 안내를 대신 삽입** (P0-2 해결)

```java
// PromptAssembler.java
if (verifiedPassages != null && !verifiedPassages.isEmpty()) {
    sb.append("\n\n<bible_verses>\n"); /* ... */
} else if (intent.requiresGrounding()) {
    sb.append("\n\n").append(NO_VERIFIED_PASSAGE_NOTICE);
    // "이번 대화에는 확인된 구절이 없습니다. 성경을 인용하거나 단정적 표현을 쓰지 마세요..."
}
```

이제 신앙 인텐트인데 근거로 삼을 구절이 없으면, 모델에게 "그럼 그냥 지어내서 신앙적 주장을
해도 되겠다"가 아니라 "인용하지 말고 공감으로만 답하거나 목회자 연결을 권하라"고 명시적으로
지시한다. 정규식으로 사후에 잡아내는 대신,애초에 그런 말을 못 하게 프롬프트 단에서 막는
접근이라 훨씬 안전하다.

**2단계 출력에 그래도 미검증 주소가 나오면 — 그 부분만 제거** (P0-3 해결의 핵심)

```java
for (String candidate : scanner.scan(text)) {
    Optional<VersePassage> resolved = verseService.findPassage(candidate);
    boolean alreadyProvided = resolved.isPresent()
            && verifiedPassages.stream().anyMatch(p -> samePassage(p, resolved.get()));
    if (!alreadyProvided) {
        // 1단계가 안 준 구절을 모델이 기억으로 추가 언급함 — 그 주소만 제거
        unverifiedReferences.add(candidate);
        finalText = finalText.replace(candidate, "");
    }
}
return new ChatReply(finalText, intent, false, verifiedPassages, List.copyOf(unverifiedReferences));
```

D-017의 "주소 하나라도 있으면 전체 텍스트 폐기"는 여기서 완전히 사라졌다. 이제는 **그
주소 문자열만** 지우고 나머지 공감 코멘트는 그대로 살아남는다. 아까 예시로 들었던 시력을
잃은 사용자 케이스라면, 이제는 공감 문장이 전부 살아있는 채로 `[요한복음 9:1-3]` DB 원문만
자연스럽게 붙어서 나간다.

---

## 왜 완전히 안전망을 없애지 않았나

1단계에서 이미 걸러졌는데 왜 2단계 출력을 또 스캔하나? 모델이 "제공받지 않은" 구절을
기억만으로 추가 언급할 가능성이 여전히 남아있기 때문이다(드물지만 0은 아님). 그래서
"기본적으로는 1단계가 다 걸러주지만, 혹시 몰라서 2단계에도 최소한의 그물을 하나 더
쳐둔다"는 이중 방어 구조를 유지했다. **완벽한 하나의 장치보다, 서로 다른 지점에 있는
두 개의 불완전한 장치가 더 안전하다**는 원칙이 07장에 이어 여기서도 반복된다.

## 남은 한계 (정직하게 기록)

- 이 설계는 "모델이 원문을 보고 쓴다"는 정합성은 챙겼지만, PRD.md가 원래 그렸던 "임베딩
  검색으로 관련 구절을 사전에 찾아온다"(Option C, RAG)는 더 큰 그림에는 아직 못 미친다.
  지금은 모델이 스스로 "이 상황에 어떤 구절이 관련 있을지" 판단해서 제안하는 방식이라,
  임베딩 기반 검색보다는 재현율이 낮을 수 있다.
- 한글로 된 가짜 성경책 이름(예: "에녹서")이 검증을 우회하는 문제는 이번에 고치려다
  되돌렸다 — 자세한 내용은 13장 참고.
