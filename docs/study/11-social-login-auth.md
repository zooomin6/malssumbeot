# 11. 소셜 로그인 — OAuth 방식 A와 JWT

> 구글·카카오로 로그인해서 우리 서버의 "회원증(JWT)"을 받기까지.
> `com.malssumbeot.auth` 패키지 전체를 다룬다. (2026-07-20 민규 학습 세션 정리)

## 1. 왜 소셜 로그인인가 — 이메일 코드 방식과의 차이

이전에 만들었던 이메일 코드(OTP) 방식과 근본이 다르다. **신원 확인의 책임을 누가 지느냐**가 갈린다.

| | 이메일 코드 (전) | 소셜 로그인 (이번) |
|---|---|---|
| 신원 확인 주체 | 우리 서버 | 구글·카카오 |
| 우리가 저장/대조하는 비밀 | 코드(OTP) | 없음 (토큰 검증만) |
| 사용자 식별 키 | 이메일 | `(provider, providerId)` |
| 가입 vs 로그인 | 보통 분리 | 하나로 통합 (upsert) |

핵심 통찰은 **"이메일에 의존하지 않는 신원"**이다. 카카오는 비즈앱 전환 전 이메일을 안 내려준다.
그래서 이메일을 신원의 기준으로 삼으면 카카오 로그인이 성립하지 않는다. 제공자가 정한 고유 id
(`providerId`)와 제공자 종류(`provider`)의 조합을 신원으로 삼으면 이메일이 없어도 로그인이 된다.
이 결정이 `User` 테이블의 유니크 제약 `(provider, provider_id)`으로 굳어져 있다.

## 2. "방식 A" — 앱이 토큰을 받아오고, 서버는 검증만 (D-022)

OAuth를 붙이는 방법은 크게 둘이다.

- **방식 B (서버 주도)**: 서버가 리다이렉트·콜백·code 교환까지 다 한다. 웹에 흔하다.
- **방식 A (앱 주도, 우리 선택)**: 앱이 구글/카카오 SDK로 로그인해 **토큰까지 받아오고**, 그
  토큰만 서버로 보낸다. 서버는 그 토큰의 진위만 검증하고 우리 JWT를 발급한다.

모바일 앱(React Native)에선 방식 A가 자연스럽다. 로그인 UI·리다이렉트를 OS/SDK가 처리하고,
서버는 검증과 회원 관리라는 본질에만 집중한다.

```
[앱] 구글/카카오에서 로그인 → 제공자 토큰 획득
      │  POST /api/auth/{provider}  { "token": "..." }
      ▼
[서버] 토큰 검증 → 회원 조회/생성 → 우리 JWT 발급 → 응답(JSON)
      ▲
[앱] JWT 저장 → 이후 요청에 첨부 → 화면 렌더링
```

**역할 분리가 핵심**: 서버는 화면을 그리지 않는다. JSON(회원 정보 + JWT)까지만 책임지고,
저장·렌더링은 앱의 몫이다.

## 3. 전략 패턴 — provider가 늘어나도 핵심 로직은 안 바뀐다

구글과 카카오는 검증 방식이 완전히 다르다.

- **구글**: 토큰이 그 자체로 서명된 JWT(ID 토큰). 구글 공개키로 **오프라인 검증**. 네트워크 호출 없음.
- **카카오**: 토큰은 그냥 열쇠. 그 열쇠로 카카오 사용자정보 API를 **실제 호출**해 진위 확인.

이 차이를 `SocialTokenVerifier` 인터페이스 뒤에 숨긴다.

```java
public interface SocialTokenVerifier {
    AuthProvider provider();          // "나는 누구 담당인가"
    SocialUser verify(String token);  // 검증하고 신원을 돌려준다
}
```

`AuthService`는 구현체가 구글인지 카카오인지 **모른다**. 생성자에서 스프링이 모든 구현 빈을
`List`로 주입해 주고, 그걸 `provider → 검증기` 맵으로 만들어 둔다:

```java
public AuthService(List<SocialTokenVerifier> verifierList, ...) {
    for (SocialTokenVerifier verifier : verifierList) {
        verifiers.put(verifier.provider(), verifier);  // {GOOGLE:…, KAKAO:…}
    }
}
```

**왜 이렇게?** 애플 로그인을 붙일 때 `AppleTokenVerifier`에 `@Component`만 달면
자동으로 이 맵에 합류한다. `AuthService`는 한 줄도 고칠 필요가 없다.
이것이 개방-폐쇄 원칙(확장엔 열려있고 수정엔 닫혀있다)의 교과서적 예다.

> 참고로 왜 AuthConfig엔 구글만 있나: 구글 검증기는 남의 라이브러리 객체(`GoogleIdTokenVerifier`)를
> audience 고정해서 조립해야 하므로 `@Bean` 설정이 필요하다. 카카오는 `@Component`로 자기 자신을
> 등록하고 필요한 URI만 application.yml에서 주입받으면 끝이라 설정 코드가 없다. 비대칭은 정상이다.

## 4. upsert — 가입과 로그인을 가르는 유일한 분기

```java
User user = userRepository.findByProviderAndProviderId(provider, social.providerId())
        .orElseGet(() -> userRepository.save(
                new User(provider, social.providerId(), social.email(), social.nickname())));
```

`(provider, providerId)`로 회원을 찾아 **있으면 로그인, 없으면 그 자리에서 만들어 가입**한다.
갈림길은 딱 여기 하나. `orElseGet`의 람다가 실행되면 신규(가입), 안 되면 기존(로그인)이다.

**`orElse`가 아니라 `orElseGet`인 이유**가 이 코드의 함정 포인트다.
- `orElse(x)`: 회원이 있든 없든 `x`를 **항상 미리 계산**한다 → 기존 회원인데도 새 User를 만들어
  저장하는 낭비·중복 사고가 난다.
- `orElseGet(() -> …)`: **없을 때만** 람다를 실행한다 → DB 저장 같은 부수효과가 있을 땐 반드시 이쪽.

`@Transactional`로 메서드 전체를 한 트랜잭션으로 묶어, 조회+저장 도중 예외가 나면 어중간하게
저장되지 않게 한다.

## 5. JWT — 로그인 이후의 신원 증명

로그인은 성공했다. 그런데 그 다음 "내 대화 목록 줘" 같은 요청마다 "넌 누구냐"를 어떻게
확인하나? 매번 구글/카카오에 다시 묻는 건 느리다. 그래서 로그인 성공 순간, 서버가
**"이 사람은 우리가 확인한 회원 N번"이라 도장 찍은 출입증**을 준다. 그게 JWT다.

```
eyJhbGc….  eyJzdWIi….  SflKxwRJ…
  헤더        내용(클레임)    서명
```

- **내용**: 회원 id(subject), provider, 발급·만료 시각.
- **서명**: 서버만 아는 비밀키로 만든 도장. 내용을 위조하면 서명이 안 맞아 **즉시 들통난다.**
  그래서 서버는 토큰만 보고도 "내가 발급한 진짜"임을 신뢰할 수 있다 — DB를 다시 안 봐도 된다.

`JwtService`가 이 출입증을 **발급(`issue`)**하고 나중에 **검사(`parse`)**한다.

```java
public String issue(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
            .subject(String.valueOf(user.getId()))        // 토큰 주인 = 회원 id
            .claim("provider", user.getProvider().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(ttl)))          // 지금 + 30일
            .signWith(key)                                 // 비밀키로 서명
            .compact();                                    // 문자열 한 줄로
}
```

### 키 없이도 부팅한다 (AnthropicConfig와 같은 철학)

`JWT_SECRET`이 없으면 **막지 않고** 경고 로그를 찍은 뒤 임시 키로 부팅한다.

```java
if (secret == null || secret.isBlank()) {
    log.warn("… 임시 키로 부팅합니다 …");
    this.key = Jwts.SIG.HS256.key().build();   // 즉석 임시 키
} else {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(UTF_8));
}
```

로컬 개발 편의를 위한 선택이다. 단, 임시 키는 재시작마다 바뀌어 **이전 발급 토큰이 전부
무효화**된다. 그래서 운영에선 32바이트 이상 `JWT_SECRET`이 필수다. ("입장 불가"가 아니라
"임시로 열되 재시작하면 리셋"이다.)

## 6. 전체 호출 트리 (구글 + 신규 회원 기준)

```
[앱] POST /api/auth/google   { "token": "eyJ..." }
 │
 ▼
① AuthController.login(provider="google", request)
 │   ├─ parseProvider("google") → AuthProvider.valueOf("GOOGLE") → GOOGLE
 │   │     (enum에 없으면 UnsupportedProviderException → 400)
 │   ├─ request.token() → "eyJ..."
 │   └─▶ authService.login(GOOGLE, "eyJ...")
 │
 ▼
② AuthService.login(GOOGLE, token)                         @Transactional
 │   ├─ verifiers.get(GOOGLE) → GoogleTokenVerifier
 │   │
 │   ├─▶ GoogleTokenVerifier.verify(token)
 │   │     ├─ googleIdTokenVerifier.verify(token)   (구글 공개키로 서명·audience·만료 검증)
 │   │     ├─ idToken.getPayload()
 │   │     └─ return new SocialUser(GOOGLE, subject, email, name)  → social
 │   │        (가짜/만료면 InvalidSocialTokenException → 401, 여기서 중단)
 │   │
 │   ├─▶ userRepository.findByProviderAndProviderId(GOOGLE, social.providerId())
 │   │     └─ (DB SELECT) → Optional.empty()   ← 신규
 │   │
 │   ├─▶ .orElseGet( () -> … )                    ← 비어있으니 실행 (가입 분기)
 │   │     ├─ new User(GOOGLE, providerId, email, nickname)
 │   │     └─▶ userRepository.save(user)
 │   │           ├─ User.onCreate()  @PrePersist   (createdAt/updatedAt 세팅)
 │   │           └─ (DB INSERT) → id 부여된 user
 │   │
 │   ├─▶ jwtService.issue(user)
 │   │     └─ Jwts.builder()…signWith(key).compact()  → JWT 문자열
 │   │
 │   └─▶ LoginResponse.from(user, jwt)
 │         └─ return new LoginResponse(jwt, provider, nickname, email)
 │
 ▼
③ Spring이 LoginResponse를 JSON으로 직렬화 (@RestController)
 │
 ▼
[앱] 200 OK  { "accessToken":"eyJ...", "provider":"GOOGLE", "nickname":…, "email":… }
     → 앱이 JWT 저장 → 화면 렌더링
```

**기존 회원(로그인)이면** ⑤의 `findBy…`가 값을 돌려주고 `orElseGet` 람다가 실행되지 않는다.
`new User`/`save`(가입)를 건너뛰고 바로 JWT 발급으로 간다. **오직 이 한 곳만** 달라진다.

### 실패는 어디서 멈추나 (방어선)

| 상황 | 막는 곳 | 응답 |
|---|---|---|
| 빈 토큰 | 컨트롤러 진입 전 `@Valid`(LoginRequest `@NotBlank`) | 400 |
| 이상한 provider (`/api/auth/facebook`) | `parseProvider` → `UnsupportedProviderException` | 400 |
| enum엔 있으나 검증기 미구현 (APPLE) | `AuthService`의 `verifiers.get()==null` | 400 |
| 가짜/만료 토큰 | `verify()` → `InvalidSocialTokenException` | 401 |

컨트롤러의 `@Valid`와 서비스의 null 체크는 노리는 빈틈이 다르다(형식 vs 미구현). 겹쳐 보이지만
둘 다 필요하다.

## 7. JWT 인증 배선 — 발급한 출입증을 실제로 검사하기 (D-023)

로그인으로 JWT를 **발급**은 했지만, 한동안 그걸 **검사하는 곳이 없었다.** `JwtService.parse()`는
만들어졌으나 호출처가 없어, `/api/chat`은 토큰 없이도·가짜 토큰으로도 부를 수 있는 무인증
상태였다. 이 절에서 "출입증 검사대"를 요청 길목에 세운다.

### 왜 HandlerInterceptor인가 (Spring Security 대신)

검사대를 놓을 자리 후보는 Servlet Filter / HandlerInterceptor / Spring Security. **HandlerInterceptor**를
골랐다 — 컨트롤러 바로 앞 길목이라 경로별 on/off가 쉽고(보호 `/api/**`, 제외 `/api/auth/**`),
Spring Security는 현 프로젝트에 없고 엔드포인트 한둘 지키는 데는 자동 설정이 과하다.
(`ChatController` 주석이 이미 이 도구를 예고했다.) 401은 새 방식 대신 기존 패턴 —
`@ResponseStatus(UNAUTHORIZED)` 예외(`UnauthenticatedException`) — 를 그대로 재사용한다.

```
요청 → [JwtAuthInterceptor.preHandle] → ChatController → ChatOrchestrator
        ├─ Authorization: Bearer <jwt> 추출 (없거나 Bearer 아님 → 401)
        ├─ jwtService.parse(jwt)  (서명·만료 실패 → 401)
        └─ 성공: userId를 request 속성(authUserId)에 실음 → 통과
```

### 왜 "매 요청 검사"가 정상이고, 메모리도 안 쌓이나

직관적으로 "매번 검사하면 부담 아닌가?" 싶지만 반대다. **HTTP는 무상태** — 서버는 "얘 아까
로그인했지"를 기억하지 않는다. 그래서 로그인 이후 요청은 매번 토큰으로 신원을 증명해야 하고,
그게 JWT 설계 그 자체다(정문 한 번이 아니라 문마다 출입증 찍기). 그리고 `parse()`는 **서명
대조(HMAC)만 하는 순수 CPU 연산** — DB·네트워크·저장이 없어 아무리 검사해도 메모리가 안 쌓인다.
오히려 메모리를 먹는 건 "서버가 세션을 기억하는 방식"이고, JWT는 그 부담을 없애려는 선택이다.

### 신원 모델 A(공존) — 이번엔 게이트만

바디의 `sessionId`(위기 sticky의 키)는 그대로 두고, JWT는 "유효한 회원만 통과"시키는 게이트로만
썼다. 인증된 userId는 request 속성에 실어 이후 기능(대화 이력)용으로 확보만 하고, `ChatController`·
위기 로직·DTO는 건드리지 않았다. 대안 B(sessionId를 userId로 통일)는 위기 sticky 의미가 바뀌고
앱 계약이 깨져 보류. **최소·비파괴 변경**을 택한 것(단순함 우선·외과수술적 변경).

### 테스트에서 드러난 슬라이스 함정

전역 `WebConfig`(WebMvcConfigurer)는 **모든 `@WebMvcTest` 슬라이스에 로드**된다 → 인터셉터 →
`JwtService`를 요구. 그래서 인증과 무관한 `AuthControllerTest`까지 컨텍스트 로딩이 깨졌고,
`@Import(JwtService.class)`로 슬라이스에 그 leaf 빈을 넣어 해결했다. (전역 MVC 설정을 추가하면
슬라이스 테스트가 그 의존성을 함께 짊어진다는 교훈.) 인증 테스트는 무토큰/형식오류/깨진토큰→401,
유효→200, 그리고 401 시 오케스트레이터 미호출을 확인한다.

이로써 "로그인 → 그 신분으로 채팅"이 백엔드에서 완성됐다. request 속성의 userId를 실제로 **소비**하는
곳(대화 이력 저장 등)은 법률 검토가 끝난 뒤 별도로 붙인다.

## 이 장에서 배울 수 있는 큰 그림

- **책임 위임**: 신원 확인은 구글/카카오에, 화면은 앱에. 서버는 검증과 회원 관리에만 집중.
- **전략 패턴 + DI**: `List<인터페이스>` 주입으로 provider별 분기를 `if/else` 없이 확장 가능하게.
- **`orElseGet` vs `orElse`**: 부수효과가 있는 기본값은 반드시 지연 평가(`orElseGet`).
- **JWT**: 서명으로 위조를 막아, 매 요청마다 DB·외부 조회 없이 신원을 증명하는 무상태 인증.
- **키 없이 부팅**: 개발 편의와 운영 안전을 로그 경고로 잇는 실전 타협.
