# 모바일 로그인 플로우 — "저장은 됐는데 왜 다시 로그인 화면이 뜨지?"

> 2026-08-15. 관련 결정: D-039.
> 이 장은 로그인 화면 하나를 만드는 데 필요한 개념들과, 그 과정에서 실제로 만난
> 버그 하나를 다룬다. 결과 코드는 `mobile/contexts/AuthContext.tsx`,
> `mobile/app/_layout.tsx`, `mobile/app/login.tsx`를 보면 된다.

---

## 1. 로그인 상태를 "누가" 들고 있어야 하는가

로그인 전에도 코드는 이미 동작하고 있었다. 채팅 화면(`chat.tsx`)이 화면에 들어올 때마다
`fetch(".../api/auth/dev-token")`로 토큰을 새로 받아 `useState`에 담아 썼다. 이 방식의
문제는 딱 하나 — **토큰이 그 화면의 지역 상태(local state)라서, 화면을 벗어나거나 앱을
재시작하면 사라진다.** 매번 새로 로그인하는 게 아니라 매번 "새로 발급"받는 것과 같다.

로그인을 제대로 만들려면 두 가지가 필요하다.

1. **여러 화면이 공유하는 상태** — 로그인 화면이 토큰을 받으면, 채팅 화면도 그 토큰을
   알아야 한다. 화면끼리 직접 값을 전달하는 건 화면이 늘어날수록 배관(plumbing)이 복잡해진다.
2. **앱을 꺼도 사라지지 않는 저장소** — 메모리(state)는 프로세스가 죽으면 사라진다.

첫 번째는 React의 **Context API**로, 두 번째는 **`expo-secure-store`**로 해결한다.

```tsx
// contexts/AuthContext.tsx
const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }) {
  const [token, setToken] = useState<string | null>(null);
  // ...
  return <AuthContext.Provider value={{ token, ... }}>{children}</AuthContext.Provider>;
}
```

`_layout.tsx`(앱의 최상위 진입점)를 `<AuthProvider>`로 감싸면, 그 아래 모든 화면이
`useAuth()` 한 줄로 같은 토큰에 접근할 수 있다. "전역 변수를 쓰면 되지 않나?"라고
생각할 수 있는데, React는 전역 변수가 바뀌어도 화면을 다시 그려주지 않는다.
Context는 **값이 바뀌면 그 값을 구독하는 화면들을 자동으로 다시 그려주는** 전역 변수라고
이해하면 된다.

### 왜 `AsyncStorage`가 아니라 `SecureStore`인가

RN 생태계에는 기기에 값을 저장하는 방법이 여럿 있다. 이 프로젝트에서 저장하는 값은
JWT(로그인 증명서)라서 `expo-secure-store`를 썼다 — OS의 보안 저장소(Android
Keystore/iOS Keychain)를 쓰는 API다. `AsyncStorage`는 암호화되지 않은 평문 저장소라
비밀번호·토큰류에는 적절하지 않다는 게 RN 커뮤니티의 일반적인 합의다. "동작은 둘 다
한다"가 아니라 "저장하는 값의 민감도에 맞는 저장소를 고른다"는 판단이 먼저다.

---

## 2. 저장은 멀쩡한데 화면은 왜 안 믿어줬나 — 라우팅 방식 자체가 문제였다

1차 구현은 이랬다. `app/index.tsx`가 렌더링되면서, 저장된 토큰이 있는지 확인하고
있으면 `/chat-entry`로, 없으면 `/login`으로 **코드로 리다이렉트**한다.

```tsx
// 1차 시도 — index.tsx가 스스로 판단해서 이동
export default function Index() {
  const { token, isLoading } = useAuth();
  if (isLoading) return <Spinner />;
  return <Redirect href={token ? "/chat-entry" : "/login"} />;
}
```

로그인 → 채팅까지는 잘 됐다. 그런데 **앱을 완전히 껐다가 다시 켜면 로그인 화면이 다시
떴다.** 이상한 건, 진단 로그를 찍어보니 `SecureStore`에서 토큰을 정확히 읽어오고
있었다는 점이다 (`저장된 토큰 읽기: eyJhbGci...` 로 정상 출력). **저장도 되고, 읽기도
되는데, 화면은 로그인 화면이었다.**

> **교훈**: 증상이 "A인데 B가 안 된다"로 보이면, A(저장/읽기)와 B(화면 전환) 사이에
> **내가 안 보고 있는 제3의 단계**가 있을 가능성을 의심해야 한다. 여기서는 "화면이
> 이미 결정된 뒤에 뒤늦게 리다이렉트를 요청하는 방식" 자체가 그 단계였다 — 앱을
> 재시작할 때 화면 상태가 복원되는 시점과, `Redirect`가 실행되는 시점 사이의 타이밍
> 문제로 추정된다. 정확한 내부 동작보다 중요한 건, **관찰(저장은 된다)이 맞다고 해서
> 그 관찰이 가리키는 원인(화면 전환 코드에 문제가 없다)까지 맞는 건 아니라는 것**이다.

### 해법 — "나중에 리다이렉트"가 아니라 "애초에 다른 화면 그룹을 그린다"

Expo Router 공식 문서(`docs.expo.dev/router/advanced/authentication`)가 권장하는
패턴은 접근 자체가 다르다. 화면이 렌더링된 *후에* 코드로 옮기는 게 아니라, **인증
여부에 따라 애초에 어떤 화면들이 존재하는지를 결정**한다.

```tsx
// 2차 — _layout.tsx가 인증 여부로 화면 "그룹" 자체를 나눈다
<Stack>
  <Stack.Protected guard={!!token}>
    <Stack.Screen name="index" />
    <Stack.Screen name="chat" />
    <Stack.Screen name="chat-entry" />
  </Stack.Protected>

  <Stack.Protected guard={!token}>
    <Stack.Screen name="login" />
  </Stack.Protected>
</Stack>
```

`guard`가 `false`인 그룹의 화면은 **애초에 스택에 존재하지 않는다.** 그래서 "지금
로그인 화면에 있는데 채팅으로 옮겨야 하나?"를 판단하는 코드 자체가 필요 없다 —
`token`이 바뀌면 리액트가 알아서 다른 그룹을 그린다. `index.tsx`도 훨씬 단순해졌다.
(이 그룹 안에서만 존재하니, 인증 안 된 사용자는 애초에 못 들어온다.)

```tsx
// index.tsx — 이제 판단 없이 그냥 다음 화면으로
export default function Index() {
  return <Redirect href="/chat-entry" />;
}
```

> **교훈**: "조건에 따라 이동시킨다"와 "조건에 따라 존재 자체를 결정한다"는 결과가
> 비슷해 보여도 다르다. 후자는 **타이밍이 개입할 여지가 없다** — 존재하지 않으면
> 애초에 보여줄 수 없으니까. 원인을 정확히 못 밝히는 타이밍 버그를 만나면, "언제
> 실행되는지"를 맞추려 하기보다 "애초에 그 타이밍이 문제 되지 않는 구조"로 바꾸는
> 방법도 있다 (15장의 "추측이 개입할 자리를 없애는 설계"와 같은 결의 교훈이다).

---

## 3. 지금은 "가짜" 로그인이다 — 그리고 그게 맞는 선택이었던 이유

이번에 만든 로그인 버튼은 실제 구글/카카오 로그인이 아니라 `POST /api/auth/dev-token`
(백엔드의 `dev` 프로파일 전용 엔드포인트)을 호출하는 임시 버튼이다. 실제 소셜 로그인을
당장 만들지 않은 이유는 기술적 제약 때문이다.

- 구글/카카오 로그인은 각 사의 **네이티브 SDK**가 필요하다.
- 네이티브 SDK는 Expo Go(지금 쓰는 개발 환경)에서 돌아가지 않는다 — Expo Go는 정해진
  네이티브 모듈 집합만 포함한 앱이라, 새 네이티브 코드를 실행하려면 **개발 빌드**로
  전환해야 한다(14장 참고).
- 카카오는 추가로 안드로이드 키해시 등록도 필요하다.

그래서 이번 작업의 범위를 **"화면·상태 구조"**로 한정했다. `AuthContext`, `SecureStore`
저장, `Stack.Protected` 가드는 실제 소셜 로그인으로 바꿔도 그대로 재사용된다 — 바뀌는
건 `login.tsx`의 버튼이 "dev-token 받기"에서 "구글 SDK 호출하기"로 바뀌는 것뿐이다.
**틀을 먼저 만들고, 안에 들어가는 내용물은 나중에 교체 가능하게 짜는 것** — 이게
이번 작업의 설계 판단이었다.

---

## 정리 — 이 장에서 가져갈 것

1. 여러 화면이 공유해야 하는 상태는 **Context**로, 앱 재시작 후에도 남아야 하는 값은
   **SecureStore** 같은 영속 저장소로 — 두 문제를 하나로 뭉뚱그리지 않는다
2. 토큰처럼 민감한 값은 저장소의 "동작 여부"가 아니라 "암호화 여부"로 골라야 한다
3. "저장/읽기는 정상인데 결과가 이상하다"는 증상은 **저장과 결과 사이의 제3의 단계**를
   의심할 신호다
4. "조건에 따라 이동시킨다"보다 "조건에 따라 애초에 존재를 결정한다"가 타이밍 버그를
   원천적으로 없앤다 (Expo Router의 `Stack.Protected`가 이 패턴의 구현체)
5. 기술적 제약(네이티브 SDK ↔ Expo Go)으로 지금 당장 못 만드는 부분은, **나중에 그대로
   교체 가능한 틀**을 먼저 만들어두고 범위를 명시적으로 미룬다
